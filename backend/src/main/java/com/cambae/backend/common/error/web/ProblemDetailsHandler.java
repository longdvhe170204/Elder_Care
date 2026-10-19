package com.cambae.backend.common.error.web;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.VersionMismatchException;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
    private static final int MYSQL_ROW_IS_REFERENCED = 1451;

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

    /**
     * Xóa mục danh mục đã được bản ghi khác tham chiếu: MySQL chặn bằng khóa ngoại RESTRICT (lỗi 1451), trả 409
     * {@code CATALOG_ITEM_IN_USE} (FR-002). Vi phạm ràng buộc khác vẫn là lỗi hệ thống.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handle(DataIntegrityViolationException e) {
        if (e.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == MYSQL_ROW_IS_REFERENCED) {
            return handle(new BusinessException(
                    HttpStatus.CONFLICT,
                    "CATALOG_ITEM_IN_USE",
                    "Mục đã được bản ghi khác tham chiếu, chỉ ngừng hiệu lực được"));
        }
        throw e;
    }

    /** Lỗi nghiệp vụ ném khi đọc thân request (ví dụ lý do chỉ có khoảng trắng) trả đúng mã của nó, không phải 400. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof BusinessException business) {
                return ResponseEntity.status(business.status())
                        .body(handle(business).getBody());
            }
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
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
