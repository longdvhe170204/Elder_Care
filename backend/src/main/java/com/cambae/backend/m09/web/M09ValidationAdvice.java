package com.cambae.backend.m09.web;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** T006: dữ liệu hợp lệ JSON nhưng thiếu trường bắt buộc trả 422 theo hợp đồng 008. */
@Order(0)
@RestControllerAdvice(basePackages = "com.cambae.backend.m09")
class M09ValidationAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidBody(MethodArgumentNotValidException exception) {
        var problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Thiếu hoặc sai trường bắt buộc");
        problem.setProperty("errorCode", "INVALID_VALUE");
        return ResponseEntity.unprocessableEntity().body(problem);
    }
}
