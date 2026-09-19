package com.print.shop.dto;

import java.time.LocalDateTime;

/**
 * 校色试印台账的一行视图：除了落账时写死的工单/印版/印刷机/结果，
 * 还带上**眼下**重核的结果。旧的通过记录留着备查，但 validNow=false 就不再当通行证。
 */
public class ProofView {

    public Long id;
    public Long jobId;
    public String jobNo;
    public String clientName;
    public String jobState;
    public Long plateId;
    public String plateCode;
    public String plateState;
    public Long pressId;
    public String pressCode;
    public String pressState;
    public String result;
    public String operator;
    public String note;
    public LocalDateTime proofTime;

    /** 只有「通过」记录才有意义：按眼下的装版和机态重核，对得上才为 true。 */
    public boolean validNow;

    /** validNow 为 false 时的人话原因，页面直接摆出来。 */
    public String invalidReason;
}
