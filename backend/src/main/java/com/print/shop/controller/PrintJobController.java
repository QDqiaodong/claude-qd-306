package com.print.shop.controller;

import com.print.shop.entity.PrintJob;
import com.print.shop.service.PrintJobService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 工单的查询接口支持多条件叠加，条件都是可选的。 */
@RestController
@RequestMapping("/api/jobs")
public class PrintJobController {

    private final PrintJobService service;

    public PrintJobController(PrintJobService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrintJob> search(
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long paperId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo) {
        return service.search(client, state, paperId, dueFrom, dueTo);
    }

    @PostMapping
    public PrintJob create(@RequestBody PrintJob form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public PrintJob update(@PathVariable Long id, @RequestBody PrintJob form) {
        form.id = id;
        return service.save(form);
    }
}
