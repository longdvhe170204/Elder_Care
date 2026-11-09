package com.eldercare.backend.health.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/** Loại dữ liệu là dòng danh mục (nhóm 1, không xóa được): không có ứng viên loại bỏ. */
record CatalogRetention(String dataType, RetentionKind retentionKind) implements RetentionProvider {
    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return Stream.empty();
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {}
}
