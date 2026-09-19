package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 校色试印台账：这一版在这台机上对过颜色的账。
 * 一条必须同时点着一张印刷工单、一块印版、一台印刷机；
 * 结果分「通过 / 不通过」，通过的旧记录一直留着备查，是否还作数以推进那一刻重核为准。
 */
@Entity
@Table(name = "color_proof")
public class ColorProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "job_id", nullable = false)
    public Long jobId;

    @Column(name = "plate_id", nullable = false)
    public Long plateId;

    @Column(name = "press_id", nullable = false)
    public Long pressId;

    /** 通过 / 不通过 */
    @Column(name = "result", nullable = false, length = 8)
    public String result;

    @Column(name = "operator", length = 32)
    public String operator;

    @Column(name = "note", length = 255)
    public String note;

    @Column(name = "proof_time", nullable = false)
    public LocalDateTime proofTime;

    /**
     * 「通过」时由服务层写成 jobId，否则为 null；库里对它有唯一索引，
     * 一张工单因此只能有一条通过。不经过服务层的裸写没有这个赋值，约束同样在。
     */
    @Column(name = "pass_job_id")
    public Long passJobId;
}
