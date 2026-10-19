package com.cambae.backend.common.error.web;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.VersionMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Trả lỗi theo RFC 9457 kèm {@code errorCode}, {@code unmetConditions}, {@code currentValue} (research R-03; FR-006,
 * FR-021). Lỗi của framework (400, 404, 405…) cũng có {@code errorCode} là tên trạng thái HTTP.
 */
@RestControllerAdvice
public class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

    static final String ERROR_CODE = "errorCode";

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handle(BusinessException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
        problem.setTitle(e.status().getReasonPhrase());
        problem.setProperty(ERROR_CODE, e.errorCode());
        if (e instanceof ConditionNotMetException c) {
            problem.setProperty("unmetConditions", c.unmetConditions());
        }
        if (e instanceof VersionMismatchException v) {
            problem.setProperty("currentValue", v.currentValue());
        }
        return ResponseEntity.status(e.status()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(ERROR_CODE))) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            problem.setProperty(ERROR_CODE, status == null ? String.valueOf(statusCode.value()) : status.name());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
