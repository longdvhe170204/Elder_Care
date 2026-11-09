package com.eldercare.backend.incident.internal.governance.persistence;

import java.util.List;

/**
 * Một loại dữ liệu lưu giữ trên một bảng riêng: {@code where} lọc dòng của loại; {@code endedAt} là biểu thức thời điểm kết
 * thúc khi không gắn hồ sơ, {@code null} khi gắn hồ sơ; {@code links} là bản ghi cùng chuỗi.
 */
public record IncidentRetentionSpec(
        String dataType,
        String sharedRowEntityType,
        String table,
        String where,
        String endedAt,
        List<IncidentRetentionLink> links) {
    boolean residentLinked() {
        return endedAt == null;
    }
}
