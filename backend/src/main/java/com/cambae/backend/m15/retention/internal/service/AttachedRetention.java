package com.cambae.backend.m15.retention.internal.service;

import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Dữ liệu của 000 đi kèm một bản ghi khác trong chuỗi: đính chính trỏ về bản gốc, nhật ký của bản ghi (FR-051a). Không
 * tự là gốc chuỗi: chỉ bị loại bỏ cùng bản ghi nó đi kèm, nên phần không gắn bản ghi nào có provider được giữ lâu hơn
 * thời hạn tối thiểu (FR-050).
 */
interface AttachedRetention extends RetentionProvider {

    /**
     * Bản ghi đi kèm {@code record}, kèm đã hết hạn theo {@code cutoff} hay chưa. {@code recordKind} là thời hạn của chính
     * {@code record}, đã hết hạn khi được hỏi: nhật ký dùng để biết hồ sơ người cao tuổi đã hết hạn chưa (FR-050).
     */
    List<Attached> attachedTo(DataRef record, RetentionKind recordKind, Instant cutoff);

    record Attached(long id, Instant at, boolean expired) {}

    @Override
    default Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return Stream.empty();
    }

    @Override
    default Set<DataRef> referencesTo(long id) {
        return Set.of();
    }
}
