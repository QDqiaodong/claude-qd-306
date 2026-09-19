package com.print.shop.dto;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /** 行锁等不到（同一张单正有人在落通过），按业务冲突报 400，不甩成 500。 */
    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<Map<String, Object>> handleLock(CannotAcquireLockException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        body.put("message", "这张工单正有人在落校色试印，稍后再试一次");
        return ResponseEntity.status(409).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        body.put("message", "服务处理失败：" + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
