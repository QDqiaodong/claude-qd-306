package com.print.shop.dto;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
