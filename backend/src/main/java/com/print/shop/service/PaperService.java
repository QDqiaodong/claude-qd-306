package com.print.shop.service;

import com.print.shop.dto.BizException;
import com.print.shop.entity.Paper;
import com.print.shop.repository.PaperRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaperService {

    private final PaperRepository papers;

    public PaperService(PaperRepository papers) {
        this.papers = papers;
    }

    public List<Paper> list(String state, String keyword) {
        return papers.findAllByOrderByIdAsc().stream()
                .filter(p -> state == null || state.isBlank() || state.equals(p.paperState))
                .filter(p -> keyword == null || keyword.isBlank()
                        || p.paperCode.contains(keyword) || p.paperName.contains(keyword))
                .toList();
    }

    @Transactional
    public Paper save(Paper form) {
        Paper origin = null;
        if (form.id != null) {
            origin = papers.findById(form.id).orElseThrow(() -> new BizException("这批纸不存在"));
            if (form.paperCode == null || form.paperCode.isBlank()) {
                form.paperCode = origin.paperCode;
            }
            if (form.paperName == null || form.paperName.isBlank()) {
                form.paperName = origin.paperName;
            }
        }
        if (form.paperCode == null || form.paperCode.isBlank()) {
            throw new BizException("纸号不能空着");
        }
        if (form.paperName == null || form.paperName.isBlank()) {
            throw new BizException("纸名不能空着");
        }
        form.paperCode = form.paperCode.trim();
        papers.findByPaperCode(form.paperCode).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("编号 " + form.paperCode + " 已经用在别的纸上了");
            }
        });
        if (form.stock != null && form.stock < 0) {
            throw new BizException("库存不能是负数");
        }
        if (origin == null) {
            form.paperState = form.paperState == null || form.paperState.isBlank() ? "充足" : form.paperState;
            return papers.save(form);
        }
        if (form.gramWeight != null) {
            origin.gramWeight = form.gramWeight;
        }
        if (form.stock != null) {
            origin.stock = form.stock;
        }
        if (form.warnLine != null) {
            origin.warnLine = form.warnLine;
        }
        if (form.paperState != null && !form.paperState.isBlank()) {
            origin.paperState = form.paperState;
        } else if (origin.warnLine != null && origin.stock != null) {
            origin.paperState = origin.stock == 0 ? "缺货" : (origin.stock < origin.warnLine ? "紧张" : "充足");
        }
        origin.paperCode = form.paperCode;
        origin.paperName = form.paperName;
        return papers.save(origin);
    }
}
