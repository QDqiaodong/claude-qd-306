package com.print.shop.controller;

import com.print.shop.dto.ProofView;
import com.print.shop.entity.ColorProof;
import com.print.shop.service.ColorProofService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 校色试印台账：落一条试印（通过/不通过），以及查台账（带眼下重核结果）。 */
@RestController
@RequestMapping("/api/proofs")
public class ColorProofController {

    private final ColorProofService service;

    public ColorProofController(ColorProofService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProofView> list(@RequestParam(required = false) Long jobId) {
        return service.list(jobId);
    }

    @PostMapping
    public ColorProof record(@RequestBody ColorProof form) {
        return service.record(form);
    }
}
