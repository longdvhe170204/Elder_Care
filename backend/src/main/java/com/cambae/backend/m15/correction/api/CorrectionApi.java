package com.cambae.backend.m15.correction.api;

import com.cambae.backend.common.Reason;
import java.util.Collection;
import java.util.Map;

/** Đính chính bản ghi nhóm 3 đã xác nhận (contracts/module-interfaces.md mục 1.6; FR-023 → FR-030). */
public interface CorrectionApi {

    /** Tạo bản đính chính theo cách A, B hoặc C (FR-026); {@code correctedContent} bắt buộc khi sửa nội dung. */
    CorrectionResult create(
            String recordType, long originalRecordId, CorrectionType type, Object correctedContent, Reason reason);

    /** Giá trị hiện hành theo lô, cho báo cáo và quy tắc tự động (FR-030). */
    Map<Long, CurrentValue> current(String recordType, Collection<Long> originalRecordIds);

    /** Chuỗi đính chính của một bản gốc (FR-028). */
    CorrectionChainView chain(String recordType, long originalRecordId);
}
