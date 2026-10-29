package com.eldercare.backend.resident.api.dto;

/**
 * Một điều kiện chưa thỏa của lệnh trạng thái.
 *
 * @param basis mã BR hoặc FR, ví dụ {@code BR-M12-03}
 * @param waivable có thể được miễn qua yêu cầu phê duyệt (chỉ 004)
 */
public record Condition(String code, String description, String basis, boolean waivable) {}
