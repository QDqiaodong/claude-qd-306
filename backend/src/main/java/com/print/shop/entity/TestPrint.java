package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 校色试印台账：一条记录 = 某张工单拿某块版在某台机上试了一版。
 * 三样缺一不可；结果只有「通过 / 不通过」。
 * 同一张待印单，眼下只认最新一条「通过」，旧的留着备查但不再当通行证。
 */
@Entity
@Table(name = "test_print")
public class TestPrint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 挂在哪张印刷工单上。 */
    @Column(name = "job_id", nullable = false)
    public Long jobId;

    /** 试的哪块印版。 */
    @Column(name = "plate_id", nullable = false)
    public Long plateId;

    /** 在哪台印刷机上试的。 */
    @Column(name = "press_id", nullable = false)
    public Long pressId;

    /** 通过 / 不通过 */
    @Column(name = "result", nullable = false, length = 8)
    public String result;

    @Column(name = "note_text", length = 200)
    public String note;

    /** 谁落的这条账，没有登录体系，手填。 */
    @Column(name = "created_by", length = 32)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public java.time.LocalDateTime createdAt;

    // —— 以下只是列表页显示用，不落库 ——

    /** 这条「通过」眼下还算不算数（版还在那台机上、机还在跑、版还在用、且是最新一条通过）。 */
    @Transient
    public Boolean validNow;

    @Transient
    public String jobNo;

    @Transient
    public String plateCode;

    @Transient
    public String pressCode;
}
