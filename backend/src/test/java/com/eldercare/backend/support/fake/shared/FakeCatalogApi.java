package com.eldercare.backend.support.fake.shared;

import static com.eldercare.backend.support.fake.shared.FakeContent.condition;
import static com.eldercare.backend.support.fake.shared.FakeContent.valid;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.VersionMismatchException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;

/** Bản giả {@link CatalogApi} giữ dữ liệu trong bộ nhớ, cho test đơn vị của module (module-interfaces 000 mục 1.8). */
public class FakeCatalogApi implements CatalogApi {

    private record Row(String kind, CatalogItem<?> item) {}

    private final Map<Long, Row> rows = new ConcurrentHashMap<>();

    @Override
    public <T> CatalogItem<T> create(SharedKind<T> kind, NewCatalogItem<T> item) {
        CatalogItem<T> created = new CatalogItem<>(
                FakeContent.IDS.incrementAndGet(),
                item.code(),
                item.name(),
                item.parentId(),
                item.sortOrder(),
                CatalogStatus.ACTIVE,
                valid(item.attributes()),
                null,
                null,
                0);
        checkUnique(kind, created);
        rows.put(created.id(), new Row(kind.code(), created));
        return created;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CatalogItem<T> require(SharedKind<T> kind, long id) {
        Row row = rows.get(id);
        if (row == null || !row.kind().equals(kind.code())) {
            throw new NotFoundException();
        }
        return (CatalogItem<T>) row.item();
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
        return this.<T>items(kind)
                .filter(i -> Objects.equals(i.parentId(), parentId) && Objects.equals(i.code(), code))
                .findFirst();
    }

    @Override
    public <T> List<CatalogItem<T>> list(SharedKind<T> kind, Long parentId, boolean activeOnly) {
        return this.<T>items(kind)
                .filter(i -> parentId == null || parentId.equals(i.parentId()))
                .filter(i -> !activeOnly || i.status() == CatalogStatus.ACTIVE)
                .sorted(Comparator.comparingInt((CatalogItem<T> i) -> i.sortOrder())
                        .thenComparing(CatalogItem::name))
                .toList();
    }

    @Override
    public <T> Change<CatalogItem<T>> update(
            SharedKind<T> kind, long id, int version, String name, int sortOrder, T attributes) {
        CatalogItem<T> before = current(kind, id, version);
        return save(
                kind,
                before,
                new CatalogItem<>(
                        id,
                        before.code(),
                        name,
                        before.parentId(),
                        sortOrder,
                        before.status(),
                        valid(attributes),
                        before.confirmedBy(),
                        before.confirmedAt(),
                        version + 1));
    }

    @Override
    public <T> Change<CatalogItem<T>> deactivate(SharedKind<T> kind, long id, int version) {
        CatalogItem<T> before = current(kind, id, version);
        if (before.status() != CatalogStatus.ACTIVE) {
            throw condition("FR-003", "Mục đã ngừng hiệu lực");
        }
        return save(kind, before, withStatus(before, CatalogStatus.INACTIVE));
    }

    @Override
    public <T> Change<CatalogItem<T>> reactivate(SharedKind<T> kind, long id, int version) {
        CatalogItem<T> before = current(kind, id, version);
        if (before.status() != CatalogStatus.INACTIVE) {
            throw condition("FR-003", "Mục đang hiệu lực");
        }
        return save(kind, before, withStatus(before, CatalogStatus.ACTIVE));
    }

    @Override
    public <T> Change<CatalogItem<T>> confirm(SharedKind<T> kind, long id, int version, long confirmedBy) {
        CatalogItem<T> before = current(kind, id, version);
        return save(
                kind,
                before,
                new CatalogItem<>(
                        id,
                        before.code(),
                        before.name(),
                        before.parentId(),
                        before.sortOrder(),
                        before.status(),
                        before.attributes(),
                        confirmedBy,
                        Instant.now(),
                        version + 1));
    }

    @Override
    public <T> CatalogItem<T> delete(SharedKind<T> kind, long id) {
        CatalogItem<T> item = require(kind, id);
        boolean referenced =
                rows.values().stream().anyMatch(r -> Objects.equals(r.item().parentId(), id));
        if (!kind.deletable() || referenced) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CATALOG_ITEM_IN_USE",
                    "Mục đã được bản ghi khác tham chiếu, chỉ ngừng hiệu lực được");
        }
        rows.remove(id);
        return item;
    }

    @SuppressWarnings("unchecked")
    private <T> java.util.stream.Stream<CatalogItem<T>> items(SharedKind<T> kind) {
        return rows.values().stream().filter(r -> r.kind().equals(kind.code())).map(r -> (CatalogItem<T>) r.item());
    }

    private <T> CatalogItem<T> current(SharedKind<T> kind, long id, int version) {
        CatalogItem<T> item = require(kind, id);
        if (item.version() != version) {
            throw new VersionMismatchException(item);
        }
        return item;
    }

    private <T> Change<CatalogItem<T>> save(SharedKind<T> kind, CatalogItem<T> before, CatalogItem<T> after) {
        checkUnique(kind, after);
        rows.put(after.id(), new Row(kind.code(), after));
        return new Change<>(before, after);
    }

    private <T> void checkUnique(SharedKind<T> kind, CatalogItem<T> item) {
        for (CatalogItem<T> other : this.<T>items(kind).toList()) {
            if (other.id() == item.id() || !Objects.equals(other.parentId(), item.parentId())) {
                continue;
            }
            if (item.status() == CatalogStatus.ACTIVE
                    && other.status() == CatalogStatus.ACTIVE
                    && other.name().equals(item.name())) {
                throw condition("BR-M15-06", "Trùng tên với mục đang Hiệu lực #" + other.id() + ": " + other.name());
            }
            if (item.code() != null && item.code().equals(other.code())) {
                throw condition("BR-M15-06", "Trùng mã với mục #" + other.id() + ": " + other.code());
            }
        }
    }

    private static <T> CatalogItem<T> withStatus(CatalogItem<T> i, CatalogStatus status) {
        return new CatalogItem<>(
                i.id(),
                i.code(),
                i.name(),
                i.parentId(),
                i.sortOrder(),
                status,
                i.attributes(),
                i.confirmedBy(),
                i.confirmedAt(),
                i.version() + 1);
    }
}
