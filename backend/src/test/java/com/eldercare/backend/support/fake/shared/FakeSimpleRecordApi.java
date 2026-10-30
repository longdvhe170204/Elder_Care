package com.eldercare.backend.support.fake.shared;

import static com.eldercare.backend.support.fake.shared.FakeContent.condition;
import static com.eldercare.backend.support.fake.shared.FakeContent.valid;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.VersionMismatchException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;

/** Bản giả {@link SimpleRecordApi} giữ dữ liệu trong bộ nhớ (module-interfaces 000 mục 1.8). */
public class FakeSimpleRecordApi implements SimpleRecordApi {

    private record Row(String kind, SimpleRecord<?> record) {}

    private final Map<Long, Row> rows = new ConcurrentHashMap<>();

    @Override
    public synchronized <T> SimpleRecord<T> create(SharedKind<T> kind, NewSimpleRecord<T> r) {
        checkUnique(r.uniqueKey(), null);
        Instant now = Instant.now();
        SimpleRecord<T> record = new SimpleRecord<>(
                FakeContent.IDS.incrementAndGet(),
                r.residentId(),
                r.parent(),
                r.status(),
                valid(r.content()),
                r.effectiveFrom(),
                r.effectiveTo(),
                r.uniqueKey(),
                r.lock() ? now : null,
                now,
                r.createdBy(),
                0);
        rows.put(record.id(), new Row(kind.code(), record));
        return record;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> SimpleRecord<T> require(SharedKind<T> kind, long id) {
        Row row = rows.get(id);
        if (row == null || !row.kind().equals(kind.code())) {
            throw new NotFoundException();
        }
        return (SimpleRecord<T>) row.record();
    }

    @Override
    public <T> SimpleRecord<T> lockForUpdate(SharedKind<T> kind, long id) {
        return require(kind, id);
    }

    @Override
    public synchronized <T> Change<SimpleRecord<T>> change(
            SharedKind<T> kind, long id, int version, RecordChange<T> c) {
        SimpleRecord<T> before = require(kind, id);
        if (before.version() != version) {
            throw new VersionMismatchException(before);
        }
        if (before.lockedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "RECORD_LOCKED", "Bản ghi đã khóa, không đổi được");
        }
        checkUnique(c.uniqueKey(), id);
        SimpleRecord<T> after = new SimpleRecord<>(
                id,
                before.residentId(),
                before.parent(),
                c.status(),
                valid(c.content()),
                before.effectiveFrom(),
                c.effectiveTo(),
                c.uniqueKey(),
                c.lock() ? Instant.now() : null,
                before.createdAt(),
                before.createdBy(),
                version + 1);
        rows.put(id, new Row(kind.code(), after));
        return new Change<>(before, after);
    }

    @Override
    public <T> Optional<SimpleRecord<T>> findByUniqueKey(SharedKind<T> kind, String uniqueKey) {
        return this.<T>records(kind)
                .filter(r -> Objects.equals(r.uniqueKey(), uniqueKey))
                .findFirst();
    }

    @Override
    public <T> List<SimpleRecord<T>> listByResident(SharedKind<T> kind, long residentId, Set<String> statuses) {
        return this.<T>records(kind)
                .filter(r -> Objects.equals(r.residentId(), residentId))
                .filter(r -> statuses.isEmpty() || statuses.contains(r.status()))
                .toList();
    }

    @Override
    public <T> List<SimpleRecord<T>> listByParent(SharedKind<T> kind, EntityRef parent) {
        return this.<T>records(kind).filter(r -> parent.equals(r.parent())).toList();
    }

    @SuppressWarnings("unchecked")
    private <T> java.util.stream.Stream<SimpleRecord<T>> records(SharedKind<T> kind) {
        return rows.values().stream()
                .filter(r -> r.kind().equals(kind.code()))
                .map(r -> (SimpleRecord<T>) r.record())
                .sorted(Comparator.comparingLong(SimpleRecord::id));
    }

    private void checkUnique(String uniqueKey, Long self) {
        if (uniqueKey != null
                && rows.values().stream()
                        .anyMatch(r -> uniqueKey.equals(r.record().uniqueKey())
                                && !Objects.equals(r.record().id(), self))) {
            throw condition("UNIQUE_KEY", "Trùng khóa duy nhất " + uniqueKey);
        }
    }
}
