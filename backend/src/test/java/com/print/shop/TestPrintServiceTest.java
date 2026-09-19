package com.print.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.print.shop.dto.BizException;
import com.print.shop.entity.Plate;
import com.print.shop.entity.Press;
import com.print.shop.entity.PrintJob;
import com.print.shop.entity.TestPrint;
import com.print.shop.repository.PlateRepository;
import com.print.shop.repository.PressRepository;
import com.print.shop.repository.PrintJobRepository;
import com.print.shop.repository.TestPrintRepository;
import com.print.shop.service.PrintJobService;
import com.print.shop.service.TestPrintService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 校色试印台账的验收用例：落账校验、上机关卡、通过失效重核、并发只留一条。
 * 直接打 service 层——页面拦不拦另说，后台这关必须都在。
 */
@SpringBootTest
class TestPrintServiceTest {

    @Autowired
    private TestPrintService testPrints;
    @Autowired
    private PrintJobService jobService;
    @Autowired
    private TestPrintRepository testPrintRepo;
    @Autowired
    private PrintJobRepository jobRepo;
    @Autowired
    private PlateRepository plateRepo;
    @Autowired
    private PressRepository pressRepo;

    private Press running;
    private Press running2;
    private Press stopped;
    private Plate plateOnRunning;
    private Plate wornPlate;

    @BeforeEach
    void setUp() {
        testPrintRepo.deleteAll();
        jobRepo.deleteAll();
        plateRepo.deleteAll();
        pressRepo.deleteAll();

        running = press("P-T1", "运行");
        running2 = press("P-T3", "运行");
        stopped = press("P-T2", "停机");
        plateOnRunning = plate("PL-T1", "在用", running);
        wornPlate = plate("PL-T2", "已磨损", running);
    }

    private Press press(String code, String state) {
        Press p = new Press();
        p.pressCode = code;
        p.pressName = code + " 机";
        p.pressState = state;
        return pressRepo.save(p);
    }

    private Plate plate(String code, String state, Press on) {
        Plate p = new Plate();
        p.plateCode = code;
        p.plateName = code + " 版";
        p.plateState = state;
        p.pressId = on == null ? null : on.id;
        return plateRepo.save(p);
    }

    private PrintJob job(String no, String state) {
        PrintJob j = new PrintJob();
        j.jobNo = no;
        j.clientName = "测试客户";
        j.plateId = plateOnRunning.id;
        j.copies = 1000;
        j.jobState = state;
        return jobRepo.save(j);
    }

    private TestPrint form(Long jobId, Long plateId, Long pressId, String result) {
        TestPrint t = new TestPrint();
        t.jobId = jobId;
        t.plateId = plateId;
        t.pressId = pressId;
        t.result = result;
        return t;
    }

    private BizException fails(TestPrint t) {
        return assertThrows(BizException.class, () -> testPrints.record(t));
    }

    private void advance(PrintJob j) {
        PrintJob form = new PrintJob();
        form.id = j.id;
        form.jobState = "印刷中";
        jobService.save(form);
    }

    // —— 落账：三样缺一不可 ——

    @Test
    void 缺任何一样都不能入账() {
        PrintJob j = job("PJ-T1", "待印");
        BizException e1 = fails(form(null, plateOnRunning.id, running.id, "通过"));
        BizException e2 = fails(form(j.id, null, running.id, "通过"));
        BizException e3 = fails(form(j.id, plateOnRunning.id, null, "通过"));
        for (BizException e : List.of(e1, e2, e3)) {
            assertTrue(e.getMessage().contains("还缺"), e.getMessage());
        }
        assertEquals(0, testPrintRepo.count());
    }

    @Test
    void 指向不存在的工单印版机器也不能入账() {
        PrintJob j = job("PJ-T2", "待印");
        assertTrue(fails(form(99999L, plateOnRunning.id, running.id, "通过")).getMessage().contains("工单不存在"));
        assertTrue(fails(form(j.id, 99999L, running.id, "通过")).getMessage().contains("印版不存在"));
        assertTrue(fails(form(j.id, plateOnRunning.id, 99999L, "通过")).getMessage().contains("印刷机不存在"));
        assertEquals(0, testPrintRepo.count());
    }

    // —— 落账：只许挂在待印单上 ——

    @Test
    void 印刷中和已完成的单子补试印要当场失败并写明走到哪一步() {
        PrintJob doing = job("PJ-T3", "印刷中");
        PrintJob done = job("PJ-T4", "已完成");
        BizException e1 = fails(form(doing.id, plateOnRunning.id, running.id, "通过"));
        BizException e2 = fails(form(done.id, plateOnRunning.id, running.id, "通过"));
        assertTrue(e1.getMessage().contains("已经走到「印刷中」"), e1.getMessage());
        assertTrue(e2.getMessage().contains("已经走到「已完成」"), e2.getMessage());
        assertEquals(0, testPrintRepo.count());
    }

    // —— 记「通过」那一刻的硬条件 ——

    @Test
    void 版不在指定机台上不能记通过() {
        PrintJob j = job("PJ-T5", "待印");
        BizException e = fails(form(j.id, plateOnRunning.id, stopped.id, "通过"));
        assertTrue(e.getMessage().contains("不在指定的"), e.getMessage());
    }

    @Test
    void 停机或封存的机台不能记通过() {
        Plate onStopped = plate("PL-T3", "在用", stopped);
        PrintJob j = job("PJ-T6", "待印");
        BizException e = fails(form(j.id, onStopped.id, stopped.id, "通过"));
        assertTrue(e.getMessage().contains("停机或封存"), e.getMessage());
    }

    @Test
    void 磨损或作废的版不能记通过() {
        PrintJob j = job("PJ-T7", "待印");
        BizException e = fails(form(j.id, wornPlate.id, running.id, "通过"));
        assertTrue(e.getMessage().contains("磨损或作废"), e.getMessage());
    }

    @Test
    void 不通过只是记账不卡装版和机态() {
        Plate loose = plate("PL-T4", "在用", null);
        PrintJob j = job("PJ-T8", "待印");
        TestPrint t = testPrints.record(form(j.id, loose.id, stopped.id, "不通过"));
        assertTrue(t.id > 0);
    }

    // —— 上机关卡 ——

    @Test
    void 没有通过记录的待印单推不进印刷中() {
        PrintJob j = job("PJ-T9", "待印");
        BizException e = assertThrows(BizException.class, () -> advance(j));
        assertTrue(e.getMessage().contains("还没有校色试印的「通过」记录"), e.getMessage());
        assertEquals("待印", jobRepo.findById(j.id).orElseThrow().jobState);
    }

    @Test
    void 有有效通过的待印单可以推进() {
        PrintJob j = job("PJ-T10", "待印");
        testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
        advance(j);
        assertEquals("印刷中", jobRepo.findById(j.id).orElseThrow().jobState);
    }

    @Test
    void 通过后版被卸到别的机器就拦下并点出版不在机上() {
        PrintJob j = job("PJ-T11", "待印");
        testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
        Plate moved = plateRepo.findById(plateOnRunning.id).orElseThrow();
        moved.pressId = stopped.id;
        plateRepo.save(moved);
        BizException e = assertThrows(BizException.class, () -> advance(j));
        assertTrue(e.getMessage().contains("不在那台机上"), e.getMessage());
        assertEquals("待印", jobRepo.findById(j.id).orElseThrow().jobState);
    }

    @Test
    void 通过后机台停机就拦下并点出机台不在跑() {
        PrintJob j = job("PJ-T12", "待印");
        testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
        Press p = pressRepo.findById(running.id).orElseThrow();
        p.pressState = "停机";
        pressRepo.save(p);
        BizException e = assertThrows(BizException.class, () -> advance(j));
        assertTrue(e.getMessage().contains("不在跑"), e.getMessage());
        assertEquals("待印", jobRepo.findById(j.id).orElseThrow().jobState);
    }

    @Test
    void 旧通过失效后可以重新校色再上机() {
        PrintJob j = job("PJ-T13", "待印");
        testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
        // 版被卸到另一台机上，旧通过失效，推不动
        Plate moved = plateRepo.findById(plateOnRunning.id).orElseThrow();
        moved.pressId = running2.id;
        plateRepo.save(moved);
        assertThrows(BizException.class, () -> advance(j));
        // 在新机台上重新校色：旧记录留档备查，新记录当通行证
        testPrints.record(form(j.id, plateOnRunning.id, running2.id, "通过"));
        advance(j);
        assertEquals("印刷中", jobRepo.findById(j.id).orElseThrow().jobState);
        assertEquals(2, testPrintRepo.count());
    }

    @Test
    void 版挪走又挪回来后旧通过按眼下状态重新算数() {
        PrintJob j = job("PJ-T17", "待印");
        testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
        Plate moved = plateRepo.findById(plateOnRunning.id).orElseThrow();
        moved.pressId = running2.id;
        plateRepo.save(moved);
        assertThrows(BizException.class, () -> advance(j));
        // 版挪回来了，按眼下的装版和机态重核就又能过
        moved.pressId = running.id;
        plateRepo.save(moved);
        advance(j);
        assertEquals("印刷中", jobRepo.findById(j.id).orElseThrow().jobState);
        assertEquals(1, testPrintRepo.count());
    }

    // —— 同一张单同时落「通过」，只留一条 ——

    @Test
    void 还有效通过时再来一条要被占住() {
        PrintJob j = job("PJ-T14", "待印");
        testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
        BizException e = fails(form(j.id, plateOnRunning.id, running.id, "通过"));
        assertTrue(e.getMessage().contains("占住"), e.getMessage());
        assertEquals(1, testPrintRepo.count());
    }

    @Test
    void 两人同时落通过只留一条() throws Exception {
        for (int round = 0; round < 3; round++) {
            PrintJob j = job("PJ-C" + round, "待印");
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            List<String> results = new CopyOnWriteArrayList<>();
            Runnable task = () -> {
                try {
                    ready.countDown();
                    go.await(5, TimeUnit.SECONDS);
                    testPrints.record(form(j.id, plateOnRunning.id, running.id, "通过"));
                    results.add("OK");
                } catch (BizException e) {
                    results.add("BIZ:" + e.getMessage());
                } catch (Exception e) {
                    results.add("ERR:" + e.getMessage());
                }
            };
            Thread a = new Thread(task);
            Thread b = new Thread(task);
            a.start();
            b.start();
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            a.join(15000);
            b.join(15000);
            long ok = results.stream().filter(r -> r.equals("OK")).count();
            long occupied = results.stream().filter(r -> r.startsWith("BIZ:") && r.contains("占住")).count();
            assertEquals(1, ok, "第 " + round + " 轮：只能有一条通过入账，实际 " + results);
            assertEquals(1, occupied, "第 " + round + " 轮：后到的人要看见对方占住，实际 " + results);
            assertEquals(1, testPrintRepo.findByJobIdOrderByIdDesc(j.id).size());
        }
    }

    // —— 工单列表上的校色标记 ——

    @Test
    void 工单列表带出校色标记() {
        PrintJob ok = job("PJ-T15", "待印");
        PrintJob none = job("PJ-T16", "待印");
        testPrints.record(form(ok.id, plateOnRunning.id, running.id, "通过"));
        List<PrintJob> list = jobService.search(null, "待印", null, null, null);
        PrintJob okRow = list.stream().filter(j -> j.id.equals(ok.id)).findFirst().orElseThrow();
        PrintJob noneRow = list.stream().filter(j -> j.id.equals(none.id)).findFirst().orElseThrow();
        assertTrue(okRow.colorPassed && okRow.colorOk);
        assertTrue(!noneRow.colorPassed && !noneRow.colorOk);
    }
}
