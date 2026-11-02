package com.eldercare.backend.administration.shared.api;

import java.util.Set;

/**
 * Loại bản ghi hiện hành và loại cũ ánh xạ về nó, gộp từ mọi {@code LegacyRecordTypeSource} (research R-15 mục 5);
 * {@code administration.correction}, {@code administration.auditlog}, {@code administration.retention} dùng khi đọc và
 * loại bỏ đính chính, nhật ký của bản ghi đã chuyển.
 */
public interface RecordTypeAliases {

    /** {@code recordType} cùng mọi loại cũ ánh xạ về nó. */
    Set<String> withLegacy(String recordType);

    /** Loại hiện hành của {@code recordType}: loại mới nếu là loại cũ, không thì chính nó. */
    String current(String recordType);
}
