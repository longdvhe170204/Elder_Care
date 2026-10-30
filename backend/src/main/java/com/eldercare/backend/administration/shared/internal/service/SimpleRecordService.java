package com.eldercare.backend.administration.shared.internal.service;

import static com.eldercare.backend.administration.shared.internal.service.SharedContent.condition;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.administration.shared.internal.domain.SimpleRow;
import com.eldercare.backend.administration.shared.internal.persistence.SimpleRecordStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link SimpleRecordApi} trên {@code simple_record} (nhóm 2, khóa theo dòng, constitution XII; research R-15). */
@Service
class SimpleRecordService implements SimpleRecordApi {

    private final SimpleRecordStore store;
    private final SharedContent content;
    private final BusinessTime time;

    SimpleRecordService(SimpleRecordStore store, SharedContent content, BusinessTime time) {
        this.store = store;
        this.content = content;
        this.time = time;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> SimpleRecord<T> create(SharedKind<T> kind, NewSimpleRecord<T> r) {
        String json = content.write(r.content());
        checkUnique(r.uniqueKey(), 0);
        Instant now = time.now();
        SimpleRow row = new SimpleRow(
                0,
                r.residentId(),
                r.parent() == null ? null : r.parent().entityType(),
                r.parent() == null ? null : r.parent().entityId(),
                r.status(),
                json,
                r.effectiveFrom(),
                r.effectiveTo(),
                r.uniqueKey(),
                r.lock() ? now : null,
                now,
                r.createdBy(),
                0);
        return require(kind, store.insert(kind.code(), kind.module(), row));
    }

    @Override
    public <T> SimpleRecord<T> require(SharedKind<T> kind, long id) {
        return record(kind, store.find(kind.code(), id).orElseThrow(NotFoundException::new));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> SimpleRecord<T> lockForUpdate(SharedKind<T> kind, long id) {
        return record(kind, store.lock(kind.code(), id).orElseThrow(NotFoundException::new));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> Change<SimpleRecord<T>> change(SharedKind<T> kind, long id, int version, RecordChange<T> c) {
        SimpleRecord<T> before = lockForUpdate(kind, id);
        if (before.version() != version) {
            throw new VersionMismatchException(before);
        }
        if (before.lockedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "RECORD_LOCKED", "Bản ghi đã khóa, không đổi được");
        }
        String json = content.write(c.content());
        checkUnique(c.uniqueKey(), id);
        store.update(id, version, c.status(), json, c.effectiveTo(), c.uniqueKey(), c.lock() ? time.now() : null);
        return new Change<>(before, require(kind, id));
    }

    @Override
    public <T> Optional<SimpleRecord<T>> findByUniqueKey(SharedKind<T> kind, String uniqueKey) {
        return store.findByUniqueKey(kind.code(), uniqueKey).map(r -> record(kind, r));
    }

    @Override
    public <T> List<SimpleRecord<T>> listByResident(SharedKind<T> kind, long residentId, Set<String> statuses) {
        return store.listByResident(kind.code(), residentId, statuses).stream()
                .map(r -> record(kind, r))
                .toList();
    }

    @Override
    public <T> List<SimpleRecord<T>> listByParent(SharedKind<T> kind, EntityRef parent) {
        return store.listByParent(kind.code(), parent.entityType(), parent.entityId()).stream()
                .map(r -> record(kind, r))
                .toList();
    }

    private void checkUnique(String uniqueKey, long self) {
        if (uniqueKey != null && store.existsUniqueKey(uniqueKey, self)) {
            throw condition("UNIQUE_KEY", "Trùng khóa duy nhất " + uniqueKey);
        }
    }

    private <T> SimpleRecord<T> record(SharedKind<T> kind, SimpleRow r) {
        EntityRef parent = r.entityType() == null ? null : new EntityRef(r.entityType(), r.entityId());
        return new SimpleRecord<>(
                r.id(),
                r.residentId(),
                parent,
                r.status(),
                content.read(kind.content(), r.content()),
                r.effectiveFrom(),
                r.effectiveTo(),
                r.uniqueKey(),
                r.lockedAt(),
                r.createdAt(),
                r.createdBy(),
                r.version());
    }
}
