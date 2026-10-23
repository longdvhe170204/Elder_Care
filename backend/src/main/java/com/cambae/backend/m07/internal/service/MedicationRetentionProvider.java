package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ dữ liệu thuốc gắn người cao tuổi (data-model mục 13, research R-11).
 * Thuộc loại {@link RetentionKind.ResidentRecord} (theo CFG-M15-03 từ trạng thái cuối).
 * Danh mục thuốc (medication) là nhóm 1 không bị loại bỏ.
 */
@Component
public class MedicationRetentionProvider implements RetentionProvider {

    public static final String TYPE = "M07.PRESCRIPTION";

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        // Giai đoạn hiện tại bảng đơn thuốc đang triển khai ở US1 (Phase 3)
        return Stream.empty();
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {
        // Thực thi xóa bởi eldercare_retention khi có dữ liệu
    }
}
