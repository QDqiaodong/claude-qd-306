package com.print.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.print.shop.service.ColorProofService;
import com.print.shop.service.PrintJobService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** 校色试印台账与上机闸门的验收场景。 */
@SpringBootTest
class ColorProofFlowTests {

    @Autowired
    private ColorProofService proofs;
    @Autowired
    private PrintJobService jobs;
    @Autowired
    private ColorProofRepository proofRepo;
    @Autowired
    private PrintJobRepository jobRepo;
    @Autowired
    private PlateRepository plateRepo;
    @Autowired
    private PressRepository pressRepo;
    @Autowired
    private DataSource dataSource;

    /** 每个场景前重建一套初始台账，避免方法之间互相串数据。 */
    @BeforeEach
    void reset() {
        try (var conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("schema-h2.sql"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ColorProof form(Long jobId, Long plateId, Long pressId, String result) {
        ColorProof f = new ColorProof();
        f.jobId = jobId;
        f.plateId = plateId;
        f.pressId = pressId;
        f.result = result;
        return f;
    }

    private void advance(Long jobId) {
        PrintJob patch = new PrintJob();
        patch.id = jobId;
        patch.jobState = "印刷中";
        jobs.save(patch);
    }

    // ---- 落账：三样必须同时点上 ------------------------------------------------

    @Test
    void 缺工单或缺印版或缺机器都不能入账() {
        BizException e1 = assertThrows(BizException.class,
                () -> proofs.record(form(null, 2L, 1L, "通过")));
        BizException e2 = assertThrows(BizException.class,
                () -> proofs.record(form(2L, null, 1L, "通过")));
        BizException e3 = assertThrows(BizException.class,
                () -> proofs.record(form(2L, 2L, null, "通过")));
        for (BizException e : List.of(e1, e2, e3)) {
            assertTrue(e.getMessage().contains("同时点上"), e.getMessage());
        }
    }

    @Test
    void 点了不存在的工单印版机器也当场失败() {
        assertTrue(assertThrows(BizException.class,
                () -> proofs.record(form(999L, 2L, 1L, "通过"))).getMessage().contains("工单不存在"));
        assertTrue(assertThrows(BizException.class,
                () -> proofs.record(form(2L, 999L, 1L, "通过"))).getMessage().contains("印版不存在"));
        assertTrue(assertThrows(BizException.class,
                () -> proofs.record(form(2L, 2L, 999L, "通过"))).getMessage().contains("印刷机不存在"));
    }

    // ---- 只许挂在待印 ---------------------------------------------------------

    @Test
    void 印刷中的单子补试印当场失败并写明走到哪一步() {
        BizException e = assertThrows(BizException.class,
                () -> proofs.record(form(1L, 1L, 1L, "通过")));
        assertTrue(e.getMessage().contains("印刷中"), e.getMessage());
    }

    @Test
    void 已完成的单子补试印同样失败并写明已完成() {
        BizException e = assertThrows(BizException.class,
                () -> proofs.record(form(4L, 4L, 3L, "不通过")));
        assertTrue(e.getMessage().contains("已完成"), e.getMessage());
    }

    // ---- 通过当场的装版/机态核法 ----------------------------------------------

    @Test
    void 停机机台不能记成通过() {
        // PL-04 在用、装在 P-03 上，P-03 停机
        BizException e = assertThrows(BizException.class,
                () -> proofs.record(form(2L, 4L, 3L, "通过")));
        assertTrue(e.getMessage().contains("停机") && e.getMessage().contains("没在跑"), e.getMessage());
    }

    @Test
    void 封存机台上的版不能记成通过() {
        // 直接把一块在用版挂到封存的 P-04 上（模拟历史装机数据），再落通过
        Plate pl4 = plateRepo.findById(4L).orElseThrow();
        pl4.pressId = 4L;
        plateRepo.save(pl4);
        BizException e = assertThrows(BizException.class,
                () -> proofs.record(form(2L, 4L, 4L, "通过")));
        assertTrue(e.getMessage().contains("封存") && e.getMessage().contains("没在跑"), e.getMessage());
    }

    @Test
    void 版不在指定机器上不能记成通过并点出装在哪() {
        // PL-02 装在 P-01，却指定 P-02（运行）
        BizException e = assertThrows(BizException.class,
                () -> proofs.record(form(5L, 2L, 2L, "通过")));
        assertTrue(e.getMessage().contains("P-01"), e.getMessage());
        assertTrue(e.getMessage().contains("不在试印指定"), e.getMessage());
    }

    @Test
    void 已磨损和已作废的版不能记成通过() {
        assertTrue(assertThrows(BizException.class,
                () -> proofs.record(form(3L, 3L, 2L, "通过"))).getMessage().contains("已磨损"));
        assertTrue(assertThrows(BizException.class,
                () -> proofs.record(form(5L, 5L, 2L, "通过"))).getMessage().contains("已作废"));
    }

    // ---- 通过后能推进，没通过不能推进 -----------------------------------------

    @Test
    void 通过且机版对得上才能推进印刷中() {
        // PJ-05：PL-02 在运行的 P-01 上
        proofs.record(form(5L, 2L, 1L, "通过"));
        assertJobState(5L, "待印");
        advance(5L);
        assertJobState(5L, "印刷中");
    }

    @Test
    void 没有通过记录推进印刷中被后台保存挡下() {
        BizException e = assertThrows(BizException.class, this::advance3);
        assertTrue(e.getMessage().contains("还没有「通过」"), e.getMessage());
        assertJobState(3L, "待印");
    }

    private void advance3() {
        advance(3L);
    }

    @Test
    void 只有不通过记录不算通行证() {
        proofs.record(form(3L, 3L, 2L, "不通过")); // 不通过不核机版态
        BizException e = assertThrows(BizException.class, this::advance3);
        assertTrue(e.getMessage().contains("还没有「通过」"), e.getMessage());
    }

    // ---- 一单一通过 -----------------------------------------------------------

    @Test
    void 同一张单第二条通过被挡且提示已被占住() {
        proofs.record(form(5L, 2L, 1L, "通过"));
        BizException e = assertThrows(BizException.class,
                () -> proofs.record(form(5L, 2L, 1L, "通过")));
        assertTrue(e.getMessage().contains("占住") || e.getMessage().contains("不能再记一条"), e.getMessage());
        assertEquals(1, proofRepo.findByJobIdOrderByProofTimeDescIdDesc(5L).stream()
                .filter(p -> "通过".equals(p.result)).count());
    }

    @Test
    void 两个人同时落通过只成一条() throws InterruptedException {
        int n = 2;
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                    proofs.record(form(3L, 2L, 1L, "通过")); // PL-02 在用、在运行的 P-01 上
                    ok.incrementAndGet();
                } catch (BizException ex) {
                    rejected.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
            t.start();
            threads.add(t);
        }
        ready.await();
        go.countDown();
        for (Thread t : threads) {
            t.join(10000);
        }
        assertEquals(1, ok.get(), "只能有一条通过入账");
        assertEquals(1, rejected.get(), "后到的人必须看到对方占住");
        assertEquals(1, proofRepo.findByJobIdOrderByProofTimeDescIdDesc(3L).size());
    }

    // ---- 旧记录留着但按眼下重核 -----------------------------------------------

    @Test
    void 版挪到别的机器后旧通过失效推进被拦并点出版不在这台机() {
        Long jobId = 5L;
        proofs.record(form(jobId, 2L, 1L, "通过"));

        Plate pl2 = plateRepo.findById(2L).orElseThrow();
        pl2.pressId = 2L; // 卸到 P-02
        plateRepo.save(pl2);

        BizException e = assertThrows(BizException.class, () -> proofs.assertReadyForPress(jobId));
        assertTrue(e.getMessage().contains("版不在这台机上"), e.getMessage());
        assertTrue(e.getMessage().contains("P-02"), e.getMessage());
        assertJobState(jobId, "待印");

        // 后台保存路径同样被拦
        BizException e2 = assertThrows(BizException.class, () -> advance(jobId));
        assertTrue(e2.getMessage().contains("版不在这台机上"), e2.getMessage());

        // 台账里旧记录还在，只是 validNow=false
        List<ProofView> views = proofs.list(jobId);
        assertEquals(1, views.size());
        assertFalse(views.get(0).validNow);
        assertTrue(views.get(0).invalidReason.contains("版不在这台机上"));
    }

    @Test
    void 版卸下后推进被拦() {
        Long jobId = 5L;
        proofs.record(form(jobId, 2L, 1L, "通过"));
        Plate pl2 = plateRepo.findById(2L).orElseThrow();
        pl2.pressId = null;
        plateRepo.save(pl2);

        BizException e = assertThrows(BizException.class, () -> proofs.assertReadyForPress(jobId));
        assertTrue(e.getMessage().contains("没装在任何机器上"), e.getMessage());
    }

    @Test
    void 机器停机后旧通过失效并点出机台不在跑() {
        Long jobId = 5L;
        proofs.record(form(jobId, 2L, 1L, "通过"));
        Press p1 = pressRepo.findById(1L).orElseThrow();
        p1.pressState = "停机";
        pressRepo.save(p1);

        BizException e = assertThrows(BizException.class, () -> proofs.assertReadyForPress(jobId));
        assertTrue(e.getMessage().contains("机台已经不在跑"), e.getMessage());

        // 恢复运行后旧记录又作数（调度认的是推进那一刻的状态，不是一劳永逸）
        p1.pressState = "运行";
        pressRepo.save(p1);
        proofs.assertReadyForPress(jobId); // 不抛即通过
        advance(jobId);
        assertJobState(jobId, "印刷中");
    }

    @Test
    void 版变磨损后旧通过失效() {
        Long jobId = 5L;
        proofs.record(form(jobId, 2L, 1L, "通过"));
        Plate pl2 = plateRepo.findById(2L).orElseThrow();
        pl2.plateState = "已磨损";
        plateRepo.save(pl2);
        BizException e = assertThrows(BizException.class, () -> proofs.assertReadyForPress(jobId));
        assertTrue(e.getMessage().contains("已磨损"), e.getMessage());
    }

    // ---- 视图 -----------------------------------------------------------------

    @Test
    void 台账视图带上眼下重核结果() {
        proofs.record(form(5L, 2L, 1L, "通过"));
        ProofView v = proofs.list(5L).get(0);
        assertTrue(v.validNow);
        assertEquals("PJ-05", v.jobNo);
        assertEquals("PL-02", v.plateCode);
        assertEquals("P-01", v.pressCode);
        assertEquals("通过", v.result);
    }

    private void assertJobState(Long jobId, String state) {
        assertEquals(state, jobRepo.findById(jobId).orElseThrow().jobState);
    }
}
