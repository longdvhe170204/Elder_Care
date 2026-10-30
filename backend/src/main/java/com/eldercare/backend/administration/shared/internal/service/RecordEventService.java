package com.eldercare.backend.administration.shared.internal.service;

import static com.eldercare.backend.administration.shared.internal.service.SharedContent.condition;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.internal.domain.EventRow;
import com.eldercare.backend.administration.shared.internal.persistence.RecordEventStore;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link RecordEventApi} trên {@code record_event} (nhóm 3; bản nháp tới khi xác nhận; research R-15). */
@Service
class RecordEventService implements RecordEventApi {

    private final RecordEventStore store;
    private final SharedContent content;

    RecordEventService(RecordEventStore store, SharedContent content) {
        this.store = store;
        this.content = content;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> RecordEvent<T> add(SharedKind<T> kind, EntityRef parent, NewRecordEvent<T> e) {
        String json = content.write(e.content());
        if (e.uniqueKey() != null && store.existsUniqueKey(e.uniqueKey())) {
            throw new AlreadyProcessedException("Đã ghi " + e.uniqueKey());
        }
        EventRow row = new EventRow(
                0,
                parent.entityType(),
                parent.entityId(),
                json,
                e.replyToId(),
                e.actorId(),
                e.bySystem(),
                e.occurredAt(),
                e.deviceAt(),
                e.confirmedAt(),
                e.floorAreaId());
        return require(kind, store.insert(kind.code(), kind.module(), row, e.uniqueKey()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> RecordEvent<T> updateDraft(SharedKind<T> kind, long id, T newContent) {
        draft(kind, id);
        store.updateDraft(id, content.write(newContent), null);
        return require(kind, id);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> RecordEvent<T> confirm(SharedKind<T> kind, long id, Instant confirmedAt) {
        EventRow row = draft(kind, id);
        store.updateDraft(id, row.content(), confirmedAt);
        return require(kind, id);
    }

    @Override
    public <T> RecordEvent<T> require(SharedKind<T> kind, long id) {
        return event(kind, store.find(kind.code(), id).orElseThrow(NotFoundException::new));
    }

    @Override
    public <T> Optional<RecordEvent<T>> findByUniqueKey(SharedKind<T> kind, String uniqueKey) {
        return store.findByUniqueKey(kind.code(), uniqueKey).map(r -> event(kind, r));
    }

    @Override
    public <T> List<RecordEvent<T>> list(SharedKind<T> kind, EntityRef parent) {
        return store.list(kind.code(), parent.entityType(), parent.entityId()).stream()
                .map(r -> event(kind, r))
                .toList();
    }

    private EventRow draft(SharedKind<?> kind, long id) {
        EventRow row = store.lock(kind.code(), id).orElseThrow(NotFoundException::new);
        if (row.confirmedAt() != null) {
            throw condition("RECORD_CONFIRMED", "Bản ghi đã xác nhận, chỉ đính chính được");
        }
        return row;
    }

    private <T> RecordEvent<T> event(SharedKind<T> kind, EventRow r) {
        return new RecordEvent<>(
                r.id(),
                new EntityRef(r.entityType(), r.entityId()),
                content.read(kind.content(), r.content()),
                r.replyToId(),
                r.actorId(),
                r.bySystem(),
                r.occurredAt(),
                r.deviceAt(),
                r.confirmedAt(),
                r.floorAreaId());
    }
}
