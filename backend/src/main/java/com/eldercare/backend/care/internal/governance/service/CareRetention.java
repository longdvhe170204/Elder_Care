package com.eldercare.backend.care.internal.governance.service;

import com.eldercare.backend.administration.retention.api.*;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.care.internal.governance.domain.CareRetentionType;
import com.eldercare.backend.care.internal.governance.persistence.CareGovernanceStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/** T008: không bịa mốc cuối nếu nguồn chưa có; materialize trước khi trả Stream. */
public class CareRetention implements RetentionProvider {
    private final CareRetentionType type;
    private final CareGovernanceStore store;
    private final RetentionDatabase database;
    private final ObjectProvider<ResidentApi> residents;

    public CareRetention(
            CareRetentionType type,
            CareGovernanceStore store,
            RetentionDatabase database,
            ObjectProvider<ResidentApi> residents) {
        this.type = type;
        this.store = store;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return type.dataType();
    }

    /** T064: task vị trí tính hạn riêng nhưng lịch sử, dấu nhắc ghi M04.TASK; đính chính, sự kiện ghi M04.CARE_RESULT. */
    @Override
    public String sharedRowEntityType() {
        return switch (type) {
            case LOCATION_TASK -> CareRetentionType.TASK.dataType();
            case LOCATION_CARE_RESULT -> CareRetentionType.CARE_RESULT.dataType();
            default -> type.dataType();
        };
    }

    @Override
    public RetentionKind retentionKind() {
        return type.profile() ? new RetentionKind.ResidentRecord() : new RetentionKind.NotResidentLinked();
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return expiredCandidates(cutoff, null, batch);
    }

    /** Trang sau mốc {@code (at, id)} (000 T112). */
    @Override
    @Transactional(readOnly = true)
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        if (batch <= 0) return Stream.empty();
        var source = residents.getIfAvailable();
        var all = store.rows(type).stream()
                .flatMap(row -> {
                    Optional<Instant> at = type.profile()
                            ? (source == null || row.residentId() == null
                                    ? Optional.empty()
                                    : source.finalStatusAt(row.residentId()))
                            : Optional.ofNullable(row.plannedAt());
                    return at
                            .filter(value -> value.isBefore(cutoff))
                            .map(value -> new Candidate(row.id(), value))
                            .stream();
                })
                .toList();
        return RetentionProvider.page(all.stream(), after, batch).toList().stream();
    }

    /** Bản ghi theo hồ sơ của người cao tuổi {@code target} (000 T112, FR-051a); loại theo vị trí không gắn hồ sơ. */
    @Override
    @Transactional(readOnly = true)
    public Set<DataRef> referencing(DataRef target) {
        if (!type.profile() || !SharedRowRetention.RESIDENT.equals(target.dataType())) return Set.of();
        return store.rowsOfResident(type, target.id()).stream()
                .map(row -> new DataRef(dataType(), row.id()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return store.references(type, id);
    }

    @Override
    public void delete(Collection<Long> ids) {
        store.delete(database.jdbc(), type, ids);
    }
}
