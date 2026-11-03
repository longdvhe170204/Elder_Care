package com.eldercare.backend.care.internal.catalog.persistence;

import static com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds.REASONS;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** T007a: mọi truy cập bảng chung qua API của chủ bảng. */
@Repository
public class NonPerformanceReasonStore {
    private final CatalogApi catalogs;

    public NonPerformanceReasonStore(CatalogApi catalogs) {
        this.catalogs = catalogs;
    }

    public List<NonPerformanceReasonType> list() {
        return catalogs.list(REASONS, null, false).stream()
                .map(NonPerformanceReasonType::of)
                .sorted(java.util.Comparator.comparingLong(NonPerformanceReasonType::id))
                .toList();
    }

    public NonPerformanceReasonType lock(long id) {
        return list().stream().filter(row -> row.id() == id).findFirst().orElseThrow(NotFoundException::new);
    }

    public NonPerformanceReasonType create(String name) {
        try {
            return NonPerformanceReasonType.of(catalogs.create(
                    REASONS, new NewCatalogItem<>(null, name, null, 0, new ReasonAttributes(false, null))));
        } catch (ConditionNotMetException duplicate) {
            throw new DataIntegrityViolationException("Trùng tên danh mục", duplicate);
        }
    }

    public NonPerformanceReasonType rename(NonPerformanceReasonType row, String name) {
        var original = catalogs.require(REASONS, row.catalogId());
        try {
            return NonPerformanceReasonType.of(catalogs.update(
                            REASONS, row.catalogId(), row.version(), name, original.sortOrder(), original.attributes())
                    .after());
        } catch (ConditionNotMetException duplicate) {
            throw new DataIntegrityViolationException("Trùng tên danh mục", duplicate);
        }
    }

    public NonPerformanceReasonType deactivate(NonPerformanceReasonType row) {
        return NonPerformanceReasonType.of(
                catalogs.deactivate(REASONS, row.catalogId(), row.version()).after());
    }
}
