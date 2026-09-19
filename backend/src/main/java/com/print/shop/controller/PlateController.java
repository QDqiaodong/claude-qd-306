package com.print.shop.controller;

import com.print.shop.entity.Plate;
import com.print.shop.service.PlateService;
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
@RequestMapping("/api/plates")
public class PlateController {

    private final PlateService service;

    public PlateController(PlateService service) {
        this.service = service;
    }

    @GetMapping
    public List<Plate> list(@RequestParam(required = false) Long pressId,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String keyword) {
        return service.list(pressId, state, keyword);
    }

    @PostMapping
    public Plate create(@RequestBody Plate form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public Plate update(@PathVariable Long id, @RequestBody Plate form) {
        form.id = id;
        return service.save(form);
    }
}
