package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 印版：一块版对应一个版面，装在印刷机上。 */
@Entity
@Table(name = "plate")
public class Plate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "plate_code", nullable = false, length = 24, unique = true)
    public String plateCode;

    @Column(name = "plate_name", nullable = false, length = 64)
    public String plateName;

    @Column(name = "plate_size", length = 16)
    public String plateSize;

    @Column(name = "press_id")
    public Long pressId;

    @Column(name = "plate_date")
    public java.time.LocalDate plateDate;

    /** 在用 / 已磨损 / 已作废 */
    @Column(name = "plate_state", nullable = false, length = 16)
    public String plateState;
}
