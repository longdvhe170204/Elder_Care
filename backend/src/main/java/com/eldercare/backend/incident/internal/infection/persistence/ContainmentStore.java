package com.eldercare.backend.incident.internal.infection.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentPeriod;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** Lưu vòng đời khoanh vùng trên bảng hồ sơ chung (R-15, T052). */
@Repository
public class ContainmentStore {
    private static final String ACTIVE = "ACTIVE";
    private static final String RELEASED = "RELEASED";
    private static final SharedKind<ContainmentContent> KIND =
            SharedKind.of("M05.CONTAINMENT", ContainmentContent.class);

    private final SimpleRecordApi records;

    public ContainmentStore(SimpleRecordApi records) {
        this.records = records;
    }

    public SimpleRecord<ContainmentContent> create(
            ContainmentScope scope, Instant startedAt, long actorId, String reason, Set<Long> incidentIds) {
        var content =
                new ContainmentContent(new ContainmentPeriod(scope, startedAt, null), reason, incidentIds, null, null);
        return records.create(
                KIND, new NewSimpleRecord<>(null, null, ACTIVE, content, null, null, null, actorId, false));
    }

    public SimpleRecord<ContainmentContent> release(
            long id, int version, Instant releasedAt, long actorId, String reason) {
        var current = records.lockForUpdate(KIND, id).content();
        var period = current.period();
        var content = new ContainmentContent(
                new ContainmentPeriod(period.scope(), period.startedAt(), releasedAt),
                current.reason(),
                current.incidentIds(),
                actorId,
                reason);
        return records.change(KIND, id, version, new RecordChange<>(RELEASED, content, null, null, true))
                .after();
    }

    public SimpleRecord<ContainmentContent> lock(long id) {
        return records.lockForUpdate(KIND, id);
    }

    public SimpleRecord<ContainmentContent> attach(long id, int version, Set<Long> incidentIds) {
        var old = records.require(KIND, id).content();
        var content = new ContainmentContent(old.period(), old.reason(), incidentIds, null, null);
        return records.change(KIND, id, version, new RecordChange<>(ACTIVE, content, null, null, false))
                .after();
    }

    public List<ContainmentScope> scopesAt(Instant at) {
        return list().stream()
                .map(row -> row.content().period())
                .filter(period -> period.includes(at))
                .map(ContainmentPeriod::scope)
                .toList();
    }

    /** Còn vùng Đang khoanh vùng chỉ gắn với sự cố này (FR-066). */
    public boolean activeOnlyFor(long incidentId) {
        return records.listByStatus(KIND, Set.of(ACTIVE)).stream()
                .anyMatch(row -> row.content().incidentIds().equals(Set.of(incidentId)));
    }

    public List<SimpleRecord<ContainmentContent>> list() {
        return records.listByStatus(KIND, Set.of(ACTIVE, RELEASED));
    }
}
