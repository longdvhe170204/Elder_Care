package com.eldercare.backend.administration.retention.api.spi;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Một loại dữ liệu được lưu giữ và loại bỏ theo thời hạn (contracts/module-interfaces.md mục 1.7, research R-11; FR-050,
 * FR-051, FR-051a). Module sở hữu loại dữ liệu cài; chỉ job loại bỏ dữ liệu hết hạn gọi {@link #delete}, qua
 * {@link com.eldercare.backend.administration.retention.api.RetentionDatabase} (tài khoản {@code eldercare_retention}).
 */
public interface RetentionProvider {

    /** Loại dữ liệu, trùng loại đối tượng trong nhật ký và loại bản ghi của đính chính (ví dụ {@code M07.MEDICATION_DOSE}). */
    String dataType();

    RetentionKind retentionKind();

    /** Bản ghi có mốc tính hạn trước {@code cutoff}, tối đa {@code batch} bản ghi. */
    Stream<Candidate> expiredCandidates(Instant cutoff, int batch);

    /** Bản ghi đang trỏ tới bản ghi {@code id} của loại này (FR-051a). */
    Set<DataRef> referencesTo(long id);

    /** Xóa bằng tài khoản {@code eldercare_retention}, trong transaction của job. */
    void delete(Collection<Long> ids);

    /** Một bản ghi đã qua hạn, kèm mốc tính hạn. */
    record Candidate(long id, Instant at) {}

    /** Tham chiếu tới một bản ghi của loại dữ liệu khác. */
    record DataRef(String dataType, long id) {}
}
