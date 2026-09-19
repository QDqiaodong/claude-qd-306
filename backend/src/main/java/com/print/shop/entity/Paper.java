package com.print.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 纸张库存。 */
@Entity
@Table(name = "paper")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "paper_code", nullable = false, length = 24, unique = true)
    public String paperCode;

    @Column(name = "paper_name", nullable = false, length = 64)
    public String paperName;

    @Column(name = "gram_weight")
    public Integer gramWeight;

    @Column(name = "stock", nullable = false)
    public Integer stock;

    @Column(name = "warn_line")
    public Integer warnLine;

    /** 充足 / 紧张 / 缺货 */
    @Column(name = "paper_state", nullable = false, length = 16)
    public String paperState;

    /** 库存罩不住这么多份就别接。 */
    public void assertEnough(int copies) {
        if (copies <= 0) {
            throw new com.print.shop.dto.BizException("印刷份数得大于 0");
        }
        if ("缺货".equals(paperState) || stock == null || stock < copies / 10) {
            throw new com.print.shop.dto.BizException("「" + paperName + "」库存不够（现有 " + stock
                    + "），先补纸再开单");
        }
    }
}
