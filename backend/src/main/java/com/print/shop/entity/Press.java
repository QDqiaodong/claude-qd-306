package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 印刷机。 */
@Entity
@Table(name = "press")
public class Press {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "press_code", nullable = false, length = 24, unique = true)
    public String pressCode;

    @Column(name = "press_name", nullable = false, length = 64)
    public String pressName;

    @Column(name = "model_text", length = 32)
    public String modelText;

    @Column(name = "operator", length = 32)
    public String operator;

    /** 运行 / 停机 / 封存 */
    @Column(name = "press_state", nullable = false, length = 16)
    public String pressState;
}
