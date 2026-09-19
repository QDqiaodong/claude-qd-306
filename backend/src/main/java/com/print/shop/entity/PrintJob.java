package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/** 印刷工单：客户、用哪批纸、上哪块版、印多少份。 */
@Entity
@Table(name = "print_job")
public class PrintJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "job_no", nullable = false, length = 24, unique = true)
    public String jobNo;

    @Column(name = "client_name", nullable = false, length = 64)
    public String clientName;

    @Column(name = "paper_id")
    public Long paperId;

    @Column(name = "plate_id")
    public Long plateId;

    @Column(name = "copies", nullable = false)
    public Integer copies;

    @Column(name = "due_date")
    public java.time.LocalDate dueDate;

    /** 待印 / 印刷中 / 已完成 */
    @Column(name = "job_state", nullable = false, length = 16)
    public String jobState;

    // —— 以下只是列表页显示用，不落库 ——

    /** 有没有落过校色「通过」。 */
    @Transient
    public Boolean colorPassed;

    /** 最新那条通过眼下还算不算数（版还在那台机上、机还在跑、版还在用）。 */
    @Transient
    public Boolean colorOk;
}
