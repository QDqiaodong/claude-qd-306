package com.print.shop.controller;

import com.print.shop.entity.Press;
import com.print.shop.service.PressService;
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
@RequestMapping("/api/presses")
public class PressController {

    private final PressService service;

    public PressController(PressService service) {
        this.service = service;
    }

    @GetMapping
    public List<Press> list(@RequestParam(required = false) String state,
                            @RequestParam(required = false) String keyword) {
        return service.list(state, keyword);
    }

    @PostMapping
    public Press create(@RequestBody Press form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public Press update(@PathVariable Long id, @RequestBody Press form) {
        form.id = id;
        return service.save(form);
    }
}
