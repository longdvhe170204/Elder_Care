package com.eldercare.backend.support.fake.shared;

import static com.eldercare.backend.support.fake.shared.FakeContent.condition;
import static com.eldercare.backend.support.fake.shared.FakeContent.valid;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Bản giả {@link RecordEventApi} giữ dữ liệu trong bộ nhớ (module-interfaces 000 mục 1.8). */
public class FakeRecordEventApi implements RecordEventApi {

    private record Row(String kind, String uniqueKey, RecordEvent<?> event) {}

    private final Map<Long, Row> rows = new ConcurrentHashMap<>();

    @Override
    public synchronized <T> RecordEvent<T> add(SharedKind<T> kind, EntityRef parent, NewRecordEvent<T> e) {
        if (e.uniqueKey() != null
                && rows.values().stream().anyMatch(r -> e.uniqueKey().equals(r.uniqueKey()))) {
            throw new AlreadyProcessedException("Đã ghi " + e.uniqueKey());
        }
        RecordEvent<T> event = new RecordEvent<>(
                FakeContent.IDS.incrementAndGet(),
                parent,
                valid(e.content()),
                e.replyToId(),
                e.actorId(),
                e.bySystem(),
                e.occurredAt(),
                e.deviceAt(),
                e.confirmedAt(),
                e.floorAreaId());
        rows.put(event.id(), new Row(kind.code(), e.uniqueKey(), event));
        return event;
    }

    @Override
    public synchronized <T> RecordEvent<T> updateDraft(SharedKind<T> kind, long id, T content) {
        RecordEvent<T> e = draft(kind, id);
        return replace(
                kind,
                id,
                new RecordEvent<>(
                        id,
                        e.parent(),
                        valid(content),
                        e.replyToId(),
                        e.actorId(),
                        e.bySystem(),
                        e.occurredAt(),
                        e.deviceAt(),
                        null,
                        e.floorAreaId()));
    }

    @Override
    public synchronized <T> RecordEvent<T> confirm(SharedKind<T> kind, long id, Instant confirmedAt) {
        RecordEvent<T> e = draft(kind, id);
        return replace(
                kind,
                id,
                new RecordEvent<>(
                        id,
                        e.parent(),
                        e.content(),
                        e.replyToId(),
                        e.actorId(),
                        e.bySystem(),
                        e.occurredAt(),
                        e.deviceAt(),
                        confirmedAt,
                        e.floorAreaId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RecordEvent<T> require(SharedKind<T> kind, long id) {
        Row row = rows.get(id);
        if (row == null || !row.kind().equals(kind.code())) {
            throw new NotFoundException();
        }
        return (RecordEvent<T>) row.event();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<RecordEvent<T>> findByUniqueKey(SharedKind<T> kind, String uniqueKey) {
        return rows.values().stream()
                .filter(r -> r.kind().equals(kind.code()) && Objects.equals(r.uniqueKey(), uniqueKey))
                .map(r -> (RecordEvent<T>) r.event())
                .findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<RecordEvent<T>> list(SharedKind<T> kind, EntityRef parent) {
        return rows.values().stream()
                .filter(r -> r.kind().equals(kind.code()) && r.event().parent().equals(parent))
                .map(r -> (RecordEvent<T>) r.event())
                .sorted(Comparator.comparing((RecordEvent<T> e) -> e.occurredAt())
                        .thenComparingLong(RecordEvent::id))
                .toList();
    }

    private <T> RecordEvent<T> draft(SharedKind<T> kind, long id) {
        RecordEvent<T> e = require(kind, id);
        if (e.confirmedAt() != null) {
            throw condition("RECORD_CONFIRMED", "Bản ghi đã xác nhận, chỉ đính chính được");
        }
        return e;
    }

    private <T> RecordEvent<T> replace(SharedKind<T> kind, long id, RecordEvent<T> event) {
        rows.put(id, new Row(kind.code(), rows.get(id).uniqueKey(), event));
        return event;
    }
}
