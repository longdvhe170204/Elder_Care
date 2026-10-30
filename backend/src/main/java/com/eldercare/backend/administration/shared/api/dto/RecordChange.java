package com.eldercare.backend.administration.shared.api.dto;

import java.time.LocalDate;

/**
 * Thay đổi của dòng {@code simple_record} chưa khóa; ghi đè cả bốn giá trị. {@code uniqueKey} {@code null} để nhả ràng
 * buộc duy nhất khi dòng hết hiệu lực; {@code lock} khóa dòng (xác nhận, chốt, đóng).
 */
public record RecordChange<T>(String status, T content, LocalDate effectiveTo, String uniqueKey, boolean lock) {}
