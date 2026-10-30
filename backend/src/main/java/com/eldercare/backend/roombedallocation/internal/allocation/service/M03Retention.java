package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.spi.ResidencyEndApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.M03GovernanceStore;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;

/** T010: hạn theo data-model mục 10; thiếu nguồn trạng thái cuối thì giữ dữ liệu, không đoán hạn. */
public class M03Retention implements RetentionProvider {
    private final String type, table;
    private final RetentionKind kind;
    private final M03GovernanceStore store;
    private final RetentionDatabase database;
    private final ObjectProvider<ResidencyEndApi> ends;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public M03Retention(
            String type,
            String table,
            RetentionKind kind,
            M03GovernanceStore store,
            RetentionDatabase database,
            ObjectProvider<ResidencyEndApi> ends,
            ParameterApi parameters,
            BusinessTime time) {
        this.type = type;
        this.table = table;
        this.kind = kind;
        this.store = store;
        this.database = database;
        this.ends = ends;
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public String dataType() {
        return type;
    }

    @Override
    public RetentionKind retentionKind() {
        return kind;
    }

    private Optional<Instant> finalAt(long residentId) {
        var api = ends.getIfAvailable();
        return api == null ? Optional.empty() : api.finalStatusAt(residentId);
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        if (batch <= 0) return Stream.empty();
        if (kind instanceof RetentionKind.ResidentRecord) {
            return store.allocations()
                    .flatMap(row -> finalAt(row.residentId())
                            .filter(at -> at.isBefore(cutoff))
                            .map(at -> new Candidate(row.allocationId(), at))
                            .stream())
                    .limit(batch);
        }
        // Lịch sử theo căn cứ phân bổ không được mất trước hạn hồ sơ của phân bổ đó (FR-051a).
        Instant profileCutoff = time.now()
                .atZone(BusinessTime.ZONE)
                .minus(parameters.read(ParameterCode.CFG_M15_03).asPeriod())
                .toInstant();
        return store.timed(table, cutoff)
                .filter(row -> row.allocationId() == null
                        || store.allocationResident(row.allocationId())
                                .flatMap(this::finalAt)
                                .filter(at -> at.isBefore(profileCutoff))
                                .isPresent())
                .limit(batch)
                .map(row -> new Candidate(row.id(), row.at()));
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return kind instanceof RetentionKind.ResidentRecord ? store.allocationReferences(id) : Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {
        store.delete(database.jdbc(), table, List.copyOf(ids));
    }
}
