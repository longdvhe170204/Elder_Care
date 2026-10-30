package com.eldercare.backend.administration.shared.internal.service;

import static com.eldercare.backend.administration.shared.internal.service.SharedContent.condition;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.internal.domain.CatalogRow;
import com.eldercare.backend.administration.shared.internal.persistence.CatalogItemStore;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link CatalogApi} trên {@code catalog_item} (FR-002, FR-003, BR-M15-06; research R-15). */
@Service
class CatalogItemService implements CatalogApi {

    private final CatalogItemStore store;
    private final SharedContent content;
    private final BusinessTime time;

    CatalogItemService(CatalogItemStore store, SharedContent content, BusinessTime time) {
        this.store = store;
        this.content = content;
        this.time = time;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> CatalogItem<T> create(SharedKind<T> kind, NewCatalogItem<T> item) {
        String attributes = content.write(item.attributes());
        checkActiveName(kind, item.parentId(), item.name(), 0);
        if (item.code() != null) {
            store.findByCode(kind.code(), item.parentId(), item.code()).ifPresent(other -> {
                throw condition("BR-M15-06", "Trùng mã với mục #" + other.id() + ": " + other.code());
            });
        }
        long id = store.insert(
                kind.code(), kind.module(), item.code(), item.name(), item.parentId(), item.sortOrder(), attributes);
        return require(kind, id);
    }

    @Override
    public <T> CatalogItem<T> require(SharedKind<T> kind, long id) {
        return item(kind, store.find(kind.code(), id).orElseThrow(NotFoundException::new));
    }

    @Override
    public <T> CatalogItem<T> requireActive(SharedKind<T> kind, long id) {
        CatalogItem<T> item = require(kind, id);
        if (item.status() != CatalogStatus.ACTIVE) {
            throw condition("FR-003", "Mục đã ngừng hiệu lực");
        }
        return item;
    }

    @Override
    public <T> Optional<CatalogItem<T>> findByCode(SharedKind<T> kind, Long parentId, String code) {
        return store.findByCode(kind.code(), parentId, code).map(row -> item(kind, row));
    }

    @Override
    public <T> List<CatalogItem<T>> list(SharedKind<T> kind, Long parentId, boolean activeOnly) {
        return store.list(kind.code(), parentId, activeOnly).stream()
                .map(row -> item(kind, row))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> Change<CatalogItem<T>> update(
            SharedKind<T> kind, long id, int version, String name, int sortOrder, T attributes) {
        CatalogItem<T> before = current(kind, id, version);
        if (before.status() == CatalogStatus.ACTIVE) {
            checkActiveName(kind, before.parentId(), name, id);
        }
        return save(
                kind,
                before,
                name,
                sortOrder,
                before.status(),
                content.write(attributes),
                before.confirmedBy(),
                before.confirmedAt());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> Change<CatalogItem<T>> deactivate(SharedKind<T> kind, long id, int version) {
        CatalogItem<T> before = current(kind, id, version);
        if (before.status() != CatalogStatus.ACTIVE) {
            throw condition("FR-003", "Mục đã ngừng hiệu lực");
        }
        return save(
                kind,
                before,
                before.name(),
                before.sortOrder(),
                CatalogStatus.INACTIVE,
                content.write(before.attributes()),
                before.confirmedBy(),
                before.confirmedAt());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> Change<CatalogItem<T>> reactivate(SharedKind<T> kind, long id, int version) {
        CatalogItem<T> before = current(kind, id, version);
        if (before.status() != CatalogStatus.INACTIVE) {
            throw condition("FR-003", "Mục đang hiệu lực");
        }
        checkActiveName(kind, before.parentId(), before.name(), id);
        return save(
                kind,
                before,
                before.name(),
                before.sortOrder(),
                CatalogStatus.ACTIVE,
                content.write(before.attributes()),
                before.confirmedBy(),
                before.confirmedAt());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> Change<CatalogItem<T>> confirm(SharedKind<T> kind, long id, int version, long confirmedBy) {
        CatalogItem<T> before = current(kind, id, version);
        return save(
                kind,
                before,
                before.name(),
                before.sortOrder(),
                before.status(),
                content.write(before.attributes()),
                confirmedBy,
                time.now());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> CatalogItem<T> delete(SharedKind<T> kind, long id) {
        CatalogItem<T> item = require(kind, id);
        if (!kind.deletable()) {
            throw inUse();
        }
        try {
            store.delete(kind.code(), id);
        } catch (DataIntegrityViolationException e) {
            // Khóa ngoại RESTRICT từ bảng khác hoặc mục con (FR-002)
            throw inUse();
        }
        return item;
    }

    private <T> CatalogItem<T> current(SharedKind<T> kind, long id, int version) {
        CatalogItem<T> item = require(kind, id);
        if (item.version() != version) {
            throw new VersionMismatchException(item);
        }
        return item;
    }

    private <T> Change<CatalogItem<T>> save(
            SharedKind<T> kind,
            CatalogItem<T> before,
            String name,
            int sortOrder,
            CatalogStatus status,
            String attributes,
            Long confirmedBy,
            Instant confirmedAt) {
        if (!store.update(
                kind.code(),
                before.id(),
                before.version(),
                name,
                sortOrder,
                status,
                attributes,
                confirmedBy,
                confirmedAt)) {
            throw new VersionMismatchException(require(kind, before.id()));
        }
        return new Change<>(before, require(kind, before.id()));
    }

    private void checkActiveName(SharedKind<?> kind, Long parentId, String name, long exceptId) {
        store.findActiveByName(kind.code(), parentId, name, exceptId).ifPresent(other -> {
            throw condition("BR-M15-06", "Trùng tên với mục đang Hiệu lực #" + other.id() + ": " + other.name());
        });
    }

    private <T> CatalogItem<T> item(SharedKind<T> kind, CatalogRow r) {
        return new CatalogItem<>(
                r.id(),
                r.code(),
                r.name(),
                r.parentId(),
                r.sortOrder(),
                r.status(),
                content.read(kind.content(), r.attributes()),
                r.confirmedBy(),
                r.confirmedAt(),
                r.version());
    }

    private static BusinessException inUse() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "CATALOG_ITEM_IN_USE",
                "Mục đã được bản ghi khác tham chiếu, chỉ ngừng hiệu lực được");
    }
}
