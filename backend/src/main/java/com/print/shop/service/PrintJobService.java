package com.print.shop.service;

import com.print.shop.dto.BizException;
import com.print.shop.entity.Paper;
import com.print.shop.entity.Plate;
import com.print.shop.entity.PrintJob;
import com.print.shop.repository.PaperRepository;
import com.print.shop.repository.PlateRepository;
import com.print.shop.repository.PrintJobRepository;
import com.print.shop.spec.PrintJobSpecs;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 印刷工单：开单前要看纸够不够、版能不能用，状态只能一步步往前推。
 */
@Service
public class PrintJobService {

    private static final List<String> FLOW = List.of("待印", "印刷中", "已完成");

    private final PrintJobRepository jobs;
    private final PaperRepository papers;
    private final PlateRepository plates;

    public PrintJobService(PrintJobRepository jobs, PaperRepository papers, PlateRepository plates) {
        this.jobs = jobs;
        this.papers = papers;
        this.plates = plates;
    }

    public List<PrintJob> search(String client, String state, Long paperId,
                                 LocalDate dueFrom, LocalDate dueTo) {
        return jobs.findAll(PrintJobSpecs.filter(client, state, paperId, dueFrom, dueTo));
    }

    @Transactional
    public PrintJob save(PrintJob form) {
        PrintJob origin = null;
        if (form.id != null) {
            origin = jobs.findById(form.id).orElseThrow(() -> new BizException("这张工单不存在"));
            if (form.jobNo == null || form.jobNo.isBlank()) {
                form.jobNo = origin.jobNo;
            }
            if (form.clientName == null || form.clientName.isBlank()) {
                form.clientName = origin.clientName;
            }
        }
        if (form.jobNo == null || form.jobNo.isBlank()) {
            throw new BizException("工单号不能空着");
        }
        form.jobNo = form.jobNo.trim();
        jobs.findByJobNo(form.jobNo).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("工单号 " + form.jobNo + " 已经开过了");
            }
        });
        if (form.clientName == null || form.clientName.isBlank()) {
            throw new BizException("客户名不能空着");
        }
        if (form.paperId != null) {
            Paper paper = papers.findById(form.paperId).orElseThrow(() -> new BizException("指定的纸张不存在"));
            paper.assertEnough(form.copies == null ? 0 : form.copies);
        }
        if (form.plateId != null) {
            Plate plate = plates.findById(form.plateId).orElseThrow(() -> new BizException("指定的印版不存在"));
            if ("已作废".equals(plate.plateState)) {
                throw new BizException("印版 " + plate.plateCode + " 已经作废，不能上机");
            }
        }
        if (origin == null) {
            form.jobState = "待印";
            return jobs.save(form);
        }
        if ("已完成".equals(origin.jobState) && form.jobState != null
                && !"已完成".equals(form.jobState)) {
            throw new BizException("这张单子已经印完了，不能再退回");
        }
        String next = form.jobState;
        if (next != null && !next.isBlank() && !next.equals(origin.jobState)) {
            int from = FLOW.indexOf(origin.jobState);
            int to = FLOW.indexOf(next);
            if (to < 0) {
                throw new BizException("状态只能是：待印 / 印刷中 / 已完成");
            }
            if (to > from + 1) {
                throw new BizException("工单得按工序走，不能从「" + origin.jobState + "」跳到「" + next + "」");
            }
            if (to < from) {
                throw new BizException("工单不能往回退");
            }
        }
        origin.clientName = form.clientName;
        if (form.paperId != null) {
            origin.paperId = form.paperId;
        }
        if (form.plateId != null) {
            origin.plateId = form.plateId;
        }
        if (form.copies != null) {
            origin.copies = form.copies;
        }
        if (form.dueDate != null) {
            origin.dueDate = form.dueDate;
        }
        if (next != null && !next.isBlank()) {
            origin.jobState = next;
        }
        return jobs.save(origin);
    }
}
