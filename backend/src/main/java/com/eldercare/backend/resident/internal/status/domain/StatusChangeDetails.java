package com.eldercare.backend.resident.internal.status.domain;

import java.util.Map;

/**
 * Cột {@code details} của dòng lịch sử trạng thái ở {@code status_history} (001 T084, data-model 001 mục 0.1): yêu cầu
 * ngoại lệ đã duyệt và các điều kiện được miễn (US4 #10), {@code null} khi không miễn điều kiện nào.
 */
public record StatusChangeDetails(Map<String, Object> conditionWaiver) {}
