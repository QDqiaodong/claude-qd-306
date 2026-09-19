package com.print.shop.service;

import com.print.shop.dto.BizException;
import com.print.shop.dto.ProofView;
import com.print.shop.entity.ColorProof;
import com.print.shop.entity.Plate;
import com.print.shop.entity.Press;
import com.print.shop.entity.PrintJob;
import com.print.shop.repository.ColorProofRepository;
import com.print.shop.repository.PlateRepository;
import com.print.shop.repository.PressRepository;
import com.print.shop.repository.PrintJobRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 校色试印台账。
 *
 * 落账规矩：
 *  - 必须同时点上已有的印刷工单、印版、印刷机，少一样都不入账；
 *  - 试印只许挂在「待印」的单子上，印刷中/已完成的单补账当场失败，并点出单子走到了哪一步；
 *  - 记成「通过」的那一刻，印版必须在用、正好装在指定机器上、机器得在运行；
 *  - 一张待印单只能有一条有效「通过」（库上唯一索引兜底，行锁把并发的第二个人挡在门外）。
 *
 * 通行证不认一劳永逸：推进印刷中时按**眼下**的装版和机态重核，
 * 版被卸到别的机器、机器停机封存、版磨损作废，旧记录留着备查但不再作数。
 */
@Service
public class ColorProofService {

    public static final String RESULT_PASS = "通过";
    public static final String RESULT_FAIL = "不通过";

    private final ColorProofRepository proofs;
    private final PrintJobRepository jobs;
    private final PlateRepository plates;
    private final PressRepository presses;

    public ColorProofService(ColorProofRepository proofs, PrintJobRepository jobs,
                             PlateRepository plates, PressRepository presses) {
        this.proofs = proofs;
        this.jobs = jobs;
        this.plates = plates;
        this.presses = presses;
    }

    /** 台账列表，带眼下重核结果；jobId 给了就只看某一张工单。 */
    public List<ProofView> list(Long jobId) {
        List<ColorProof> rows = jobId == null
                ? proofs.findAllByOrderByProofTimeDescIdDesc()
                : proofs.findByJobIdOrderByProofTimeDescIdDesc(jobId);
        Map<Long, PrintJob> jobMap = new HashMap<>();
        Map<Long, Plate> plateMap = new HashMap<>();
        Map<Long, Press> pressMap = new HashMap<>();
        return rows.stream().map(p -> {
            PrintJob job = jobMap.computeIfAbsent(p.jobId,
                    id -> jobs.findById(id).orElse(null));
            Plate plate = plateMap.computeIfAbsent(p.plateId,
                    id -> plates.findById(id).orElse(null));
            Press press = pressMap.computeIfAbsent(p.pressId,
                    id -> presses.findById(id).orElse(null));
            return toView(p, job, plate, press);
        }).toList();
    }

    @Transactional
    public ColorProof record(ColorProof form) {
        // 1) 三样必须同时点上，少一样都不能入账
        if (form.jobId == null || form.plateId == null || form.pressId == null) {
            throw new BizException("落试印必须同时点上印刷工单、印版和印刷机，少了哪一样都不能入账");
        }
        if (form.result == null || form.result.isBlank()) {
            throw new BizException("试印结果得选「通过」或「不通过」");
        }
        if (!RESULT_PASS.equals(form.result) && !RESULT_FAIL.equals(form.result)) {
            throw new BizException("试印结果只能是「通过」或「不通过」");
        }

        // 2) 工单、印版、机器都得是台账里已有的
        PrintJob job = jobs.findById(form.jobId)
                .orElseThrow(() -> new BizException("点的印刷工单不存在，先把工单开出来"));
        Plate plate = plates.findById(form.plateId)
                .orElseThrow(() -> new BizException("点的印版不存在，先登记印版"));
        Press press = presses.findById(form.pressId)
                .orElseThrow(() -> new BizException("点的印刷机不存在，先登记机器"));

        // 3) 试印只许挂在「待印」的单子上，已经走下去的当场失败并写明走到哪一步
        if (!"待印".equals(job.jobState)) {
            throw new BizException("工单 " + job.jobNo + " 已经走到「" + job.jobState
                    + "」，试印只许补在还停在待印的单子上，这条不能入账");
        }

        // 4) 记成通过的那一刻：版在用、正好装在指定机器上、机器在运行
        if (RESULT_PASS.equals(form.result)) {
            assertPlateUsableOnPress(plate, press);
        }

        // 5) 一张待印单只能有一条有效通过：行锁串行 + 唯一索引兜底
        if (RESULT_PASS.equals(form.result)) {
            proofs.lockJob(job.id); // 给工单行上写锁，并发的第二个请求在这等
            proofs.findFirstByJobIdAndResultOrderByProofTimeDescIdDesc(job.id, RESULT_PASS)
                    .ifPresent(existing -> {
                        throw new BizException("工单 " + job.jobNo
                                + " 已经有一条「通过」的校色试印了，对方已经占住这张单的通过名额，不能再记一条");
                    });
        }

        form.proofTime = LocalDateTime.now();
        form.passJobId = RESULT_PASS.equals(form.result) ? form.jobId : null;
        try {
            return proofs.save(form);
        } catch (DataIntegrityViolationException dup) {
            // 两个人同时落通过，总有一个撞唯一索引 uk_proof_pass_job
            throw new BizException("工单 " + job.jobNo
                    + " 的「通过」名额刚刚已经被另一条试印占住了，你这条不能同时算数");
        }
    }

    /**
     * 推进「待印 → 印刷中」的闸门：必须有通过记录，且按眼下的装版和机态重核仍然对得上。
     * 不满足直接抛 BizException，后台保存和页面走的是同一个方法，谁也绕不过去。
     */
    @Transactional(readOnly = true)
    public void assertReadyForPress(Long jobId) {
        PrintJob job = jobs.findById(jobId)
                .orElseThrow(() -> new BizException("这张工单不存在"));
        ColorProof pass = proofs
                .findFirstByJobIdAndResultOrderByProofTimeDescIdDesc(jobId, RESULT_PASS)
                .orElseThrow(() -> new BizException("工单 " + job.jobNo
                        + " 还没有「通过」的校色试印记录，先把这一版在这台机上对过颜色再上机"));
        Plate plate = plates.findById(pass.plateId).orElse(null);
        Press press = presses.findById(pass.pressId).orElse(null);
        String reason = recheckInvalidReason(pass, job, plate, press);
        if (reason != null) {
            throw new BizException(reason);
        }
    }

    /** 版在用、正好装在指定机器、机器运行——落通过当场的核法。 */
    private void assertPlateUsableOnPress(Plate plate, Press press) {
        if ("已磨损".equals(plate.plateState) || "已作废".equals(plate.plateState)) {
            throw new BizException("印版 " + plate.plateCode + " 已经「" + plate.plateState
                    + "」，不能记成通过");
        }
        if (plate.pressId == null) {
            throw new BizException("印版 " + plate.plateCode + " 现在还没装在任何机器上，不能记成通过");
        }
        if (!press.id.equals(plate.pressId)) {
            Press mounted = presses.findById(plate.pressId).orElse(null);
            String where = mounted == null ? "另一台机器" : "印刷机 " + mounted.pressCode;
            throw new BizException("印版 " + plate.plateCode + " 现在装在" + where
                    + "上，不在试印指定的 " + press.pressCode + " 上，不能记成通过");
        }
        if (!"运行".equals(press.pressState)) {
            throw new BizException("印刷机 " + press.pressCode + " 现在是「" + press.pressState
                    + "」，机台没在跑，不能记成通过");
        }
    }

    /** 推进那一刻按眼下状态重核；对不上返回原因（人话），对得上返回 null。 */
    private String recheckInvalidReason(ColorProof pass, PrintJob job, Plate plate, Press press) {
        if (plate == null) {
            return "校色时用的印版已经不在台账里了，旧试印不再作数，先重新落一条校色试印";
        }
        if (press == null) {
            return "校色时的印刷机已经不在台账里了，旧试印不再作数，先重新落一条校色试印";
        }
        // 先点出版在不在这台机上
        if (plate.pressId == null || !press.id.equals(plate.pressId)) {
            String where = plate.pressId == null
                    ? "现在已经卸下，没装在任何机器上"
                    : presses.findById(plate.pressId)
                        .map(m -> "现在装在 " + m.pressCode + " 上").orElse("现在装在别的机器上");
            return "工单 " + job.jobNo + " 校色时用的印版 " + plate.plateCode + where
                    + "，不在指定的 " + press.pressCode + " 上了——版不在这台机上，旧试印不能当通行证";
        }
        // 再点出机台还在不在跑
        if (!"运行".equals(press.pressState)) {
            return "工单 " + job.jobNo + " 校色时的印刷机 " + press.pressCode + " 现在是「"
                    + press.pressState + "」——机台已经不在跑，旧试印不能当通行证";
        }
        // 版也得还是在用
        if (!"在用".equals(plate.plateState)) {
            return "工单 " + job.jobNo + " 校色时用的印版 " + plate.plateCode + " 现在是「"
                    + plate.plateState + "」，旧试印不能当通行证";
        }
        return null;
    }

    private ProofView toView(ColorProof p, PrintJob job, Plate plate, Press press) {
        ProofView v = new ProofView();
        v.id = p.id;
        v.jobId = p.jobId;
        v.plateId = p.plateId;
        v.pressId = p.pressId;
        v.result = p.result;
        v.operator = p.operator;
        v.note = p.note;
        v.proofTime = p.proofTime;
        if (job != null) {
            v.jobNo = job.jobNo;
            v.clientName = job.clientName;
            v.jobState = job.jobState;
        }
        if (plate != null) {
            v.plateCode = plate.plateCode;
            v.plateState = plate.plateState;
        }
        if (press != null) {
            v.pressCode = press.pressCode;
            v.pressState = press.pressState;
        }
        if (RESULT_PASS.equals(p.result)) {
            v.invalidReason = recheckInvalidReason(p, job, plate, press);
            v.validNow = v.invalidReason == null;
        }
        return v;
    }
}
