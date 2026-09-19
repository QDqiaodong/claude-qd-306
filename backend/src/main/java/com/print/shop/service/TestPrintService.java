package com.print.shop.service;

import com.print.shop.dto.BizException;
import com.print.shop.entity.Plate;
import com.print.shop.entity.Press;
import com.print.shop.entity.PrintJob;
import com.print.shop.entity.TestPrint;
import com.print.shop.repository.PlateRepository;
import com.print.shop.repository.PressRepository;
import com.print.shop.repository.PrintJobRepository;
import com.print.shop.repository.TestPrintRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 校色试印台账。两条铁律：
 * 1) 落账时三样（工单 / 印版 / 印刷机）缺一不可，且单子必须还停在「待印」；
 *    记「通过」那一刻，版必须正好装在这台机上、机器在跑、版在用。
 * 2) 通过不是一劳永逸：推进印刷中的那一刻按眼下的装版和机态重核，
 *    对不上就拦（调度的规矩，不认车间说的「试过一次管到印完」）。
 */
@Service
public class TestPrintService {

    private final TestPrintRepository testPrints;
    private final PrintJobRepository jobs;
    private final PlateRepository plates;
    private final PressRepository presses;

    public TestPrintService(TestPrintRepository testPrints, PrintJobRepository jobs,
                            PlateRepository plates, PressRepository presses) {
        this.testPrints = testPrints;
        this.jobs = jobs;
        this.plates = plates;
        this.presses = presses;
    }

    public List<TestPrint> list(Long jobId) {
        List<TestPrint> rows = jobId == null
                ? testPrints.findAllByOrderByIdDesc()
                : testPrints.findByJobIdOrderByIdDesc(jobId);
        decorate(rows);
        return rows;
    }

    @Transactional
    public TestPrint record(TestPrint form) {
        List<String> missing = new java.util.ArrayList<>();
        if (form.jobId == null) {
            missing.add("印刷工单");
        }
        if (form.plateId == null) {
            missing.add("印版");
        }
        if (form.pressId == null) {
            missing.add("印刷机");
        }
        if (!missing.isEmpty()) {
            throw new BizException("落一条试印必须同时点上工单、印版和印刷机，现在还缺："
                    + String.join("、", missing));
        }
        // 先锁工单行（本事务第一条 SQL 就得是它）：两个人同时给同一张单落「通过」，
        // 后到的人在这里排队，等前面提交后再往下读，自然看见对方已经占住。
        PrintJob job = jobs.findByIdForUpdate(form.jobId)
                .orElseThrow(() -> new BizException("指定的印刷工单不存在"));
        Plate plate = plates.findById(form.plateId)
                .orElseThrow(() -> new BizException("指定的印版不存在"));
        Press press = presses.findById(form.pressId)
                .orElseThrow(() -> new BizException("指定的印刷机不存在"));

        if (!"待印".equals(job.jobState)) {
            throw new BizException("工单 " + job.jobNo + " 已经走到「" + job.jobState
                    + "」了，试印只能落在还停在「待印」的单子上");
        }
        if (form.result == null || form.result.isBlank()) {
            throw new BizException("试印结果不能空着，要么「通过」要么「不通过」");
        }
        if (!"通过".equals(form.result) && !"不通过".equals(form.result)) {
            throw new BizException("试印结果只能是「通过」或「不通过」");
        }

        if ("通过".equals(form.result)) {
            if (!form.pressId.equals(plate.pressId)) {
                String where = plate.pressId == null
                        ? "现在没装在任何机器上"
                        : "现在装在「" + pressCodeOf(plate.pressId) + "」上";
                throw new BizException("印版 " + plate.plateCode + " " + where
                        + "，不在指定的「" + press.pressCode + "」上，对不上就不能记通过");
            }
            if (!"运行".equals(press.pressState)) {
                throw new BizException("印刷机「" + press.pressName + "」现在是「" + press.pressState
                        + "」，停机或封存的机台不能记通过");
            }
            if (!"在用".equals(plate.plateState)) {
                throw new BizException("印版 " + plate.plateCode + " 已经是「" + plate.plateState
                        + "」，磨损或作废的版不能记通过");
            }
            testPrints.findFirstByJobIdAndResultOrderByIdDesc(job.id, "通过").ifPresent(old -> {
                if (usableNow(old, plateById(), pressById())) {
                    throw new BizException("这张单已经有一条还在生效的「通过」了（" + old.createdAt
                            + " 落的），对方已经占住，后到这条没记上");
                }
            });
        }

        TestPrint t = new TestPrint();
        t.jobId = job.id;
        t.plateId = plate.id;
        t.pressId = press.id;
        t.result = form.result;
        t.note = form.note == null || form.note.isBlank() ? null : form.note.trim();
        t.createdBy = form.createdBy == null || form.createdBy.isBlank() ? null : form.createdBy.trim();
        t.createdAt = java.time.LocalDateTime.now();
        return testPrints.save(t);
    }

    /**
     * 工单从「待印」推「印刷中」之前的关卡：按眼下的装版和机态重核最新一条通过。
     * 旧记录留着备查，但版挪了、机停了、版废了，这条通过就不再是通行证。
     */
    public void assertPassUsable(Long jobId, String jobNo) {
        TestPrint pass = testPrints.findFirstByJobIdAndResultOrderByIdDesc(jobId, "通过")
                .orElseThrow(() -> new BizException("工单 " + jobNo
                        + " 还没有校色试印的「通过」记录，不能推进印刷中"));
        Plate plate = plates.findById(pass.plateId)
                .orElseThrow(() -> new BizException("校色记录里的印版已经找不到了，得重新校色"));
        if (!pass.pressId.equals(plate.pressId)) {
            throw new BizException("校色通过时印版 " + plate.plateCode + " 装在「"
                    + pressCodeOf(pass.pressId) + "」上，现在它已经不在那台机上了，"
                    + "这次通过不能再当通行证，得重新校色");
        }
        Press press = presses.findById(pass.pressId)
                .orElseThrow(() -> new BizException("校色记录里的印刷机已经找不到了，得重新校色"));
        if (!"运行".equals(press.pressState)) {
            throw new BizException("当时校色的机台「" + press.pressCode + "」现在是「" + press.pressState
                    + "」，已经不在跑了，这次通过不能再当通行证，得重新校色");
        }
        if (!"在用".equals(plate.plateState)) {
            throw new BizException("印版 " + plate.plateCode + " 现在是「" + plate.plateState
                    + "」，这次通过不能再当通行证，得重新校色");
        }
    }

    /** 给工单列表补上「已校色」标记：colorPassed = 落过通过；colorOk = 这条通过眼下还算数。 */
    public void markColorFlags(List<PrintJob> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, TestPrint> latestPassByJob = latestPassByJob();
        Map<Long, Plate> plateById = plateById();
        Map<Long, Press> pressById = pressById();
        for (PrintJob j : list) {
            TestPrint pass = latestPassByJob.get(j.id);
            j.colorPassed = pass != null;
            j.colorOk = pass != null && usableNow(pass, plateById, pressById);
        }
    }

    // —— 内部小工具 ——

    private void decorate(List<TestPrint> rows) {
        if (rows.isEmpty()) {
            return;
        }
        Map<Long, TestPrint> latestPassByJob = latestPassByJob();
        Map<Long, Plate> plateById = plateById();
        Map<Long, Press> pressById = pressById();
        Map<Long, PrintJob> jobById = jobs.findAll().stream()
                .collect(Collectors.toMap(j -> j.id, Function.identity()));
        for (TestPrint t : rows) {
            PrintJob j = jobById.get(t.jobId);
            Plate p = plateById.get(t.plateId);
            Press pr = pressById.get(t.pressId);
            t.jobNo = j == null ? null : j.jobNo;
            t.plateCode = p == null ? null : p.plateCode;
            t.pressCode = pr == null ? null : pr.pressCode;
            TestPrint latest = latestPassByJob.get(t.jobId);
            t.validNow = "通过".equals(t.result) && latest != null && latest.id.equals(t.id)
                    && usableNow(t, plateById, pressById);
        }
    }

    /** 一张单眼下只认最新一条「通过」，旧的留档备查。 */
    private Map<Long, TestPrint> latestPassByJob() {
        Map<Long, TestPrint> map = new HashMap<>();
        for (TestPrint t : testPrints.findByResult("通过")) {
            map.merge(t.jobId, t, (a, b) -> a.id > b.id ? a : b);
        }
        return map;
    }

    /** 这条通过眼下还算不算数：版还装在原来那台机上、那台机还在跑、版还在用。 */
    private boolean usableNow(TestPrint pass, Map<Long, Plate> plateById, Map<Long, Press> pressById) {
        Plate plate = plateById.get(pass.plateId);
        if (plate == null || !pass.pressId.equals(plate.pressId)) {
            return false;
        }
        Press press = pressById.get(pass.pressId);
        if (press == null || !"运行".equals(press.pressState)) {
            return false;
        }
        return "在用".equals(plate.plateState);
    }

    private Map<Long, Plate> plateById() {
        return plates.findAll().stream().collect(Collectors.toMap(p -> p.id, Function.identity()));
    }

    private Map<Long, Press> pressById() {
        return presses.findAll().stream().collect(Collectors.toMap(p -> p.id, Function.identity()));
    }

    private String pressCodeOf(Long pressId) {
        return presses.findById(pressId).map(p -> p.pressCode).orElse("未知机台");
    }
}
