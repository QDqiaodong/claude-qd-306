package com.print.shop.controller;

import com.print.shop.entity.Paper;
import com.print.shop.service.PaperService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService service;

    public PaperController(PaperService service) {
        this.service = service;
    }

    @GetMapping
    public List<Paper> list(@RequestParam(required = false) String state,
                            @RequestParam(required = false) String keyword) {
        return service.list(state, keyword);
    }

    @PostMapping
    public Paper create(@RequestBody Paper form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public Paper update(@PathVariable Long id, @RequestBody Paper form) {
        form.id = id;
        return service.save(form);
    }
}
