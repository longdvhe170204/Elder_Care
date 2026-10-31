package com.eldercare.backend.care.internal.catalog.persistence;

import static com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds.publicId;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.error.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/** Chỉ adapter của hai danh mục T007,không đọc SQL của M15. */
@Repository
public class CareCatalogStore {
    private final CatalogApi catalogs;

    public CareCatalogStore(CatalogApi catalogs) {
        this.catalogs = catalogs;
    }

    public <T> List<CatalogItem<T>> list(SharedKind<T> kind) {
        return catalogs.list(kind, null, false);
    }

    public <T> CatalogItem<T> require(SharedKind<T> kind, long id) {
        return list(kind).stream()
                .filter(row -> publicId(row) == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public <T> CatalogItem<T> create(SharedKind<T> kind, String name, T attributes) {
        try {
            return catalogs.create(kind, new NewCatalogItem<>(null, name, null, 0, attributes));
        } catch (ConditionNotMetException duplicate) {
            throw duplicate();
        }
    }

    public <T> CatalogItem<T> update(SharedKind<T> kind, CatalogItem<T> before, String name, T attributes) {
        try {
            return catalogs.update(kind, before.id(), before.version(), name, before.sortOrder(), attributes)
                    .after();
        } catch (ConditionNotMetException duplicate) {
            throw duplicate();
        }
    }

    public <T> CatalogItem<T> deactivate(SharedKind<T> kind, CatalogItem<T> before) {
        return catalogs.deactivate(kind, before.id(), before.version()).after();
    }

    private BusinessException duplicate() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Tên danh mục đang hiệu lực đã tồn tại");
    }
}
