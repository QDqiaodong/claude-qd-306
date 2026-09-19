package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
