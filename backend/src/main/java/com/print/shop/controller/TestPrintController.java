package com.print.shop.controller;

import com.print.shop.dto.BizException;
import com.print.shop.entity.TestPrint;
import com.print.shop.service.TestPrintService;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 校色试印台账：查账、落账。落账的校验全在 service，页面拦不住的后台一样拦。 */
@RestController
@RequestMapping("/api/test-prints")
public class TestPrintController {

    private final TestPrintService service;

    public TestPrintController(TestPrintService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestPrint> list(@RequestParam(required = false) Long jobId) {
        return service.list(jobId);
    }

    @PostMapping
    public TestPrint create(@RequestBody TestPrint form) {
        try {
            return service.record(form);
        } catch (DataIntegrityViolationException dup) {
            // 极端情况下两个人同时落「通过」，数据库约束兜住：只留先到的。
            throw new BizException("这张单的「通过」刚被别人抢先落上了，后到这条没记上");
        }
    }
}
