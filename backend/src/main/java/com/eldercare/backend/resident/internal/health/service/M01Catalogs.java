package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.resident.internal.health.domain.CatalogOrigin;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Bốn danh mục của M01 ở bảng chung {@code catalog_item} (001 T084; data-model 001 mục 0.1): mức chăm sóc, mức độ dị
 * ứng, nguồn thông tin, nhu cầu chăm sóc. Dị nguyên chưa chuyển (id được 006 lưu, chờ A và D duyệt).
 */
@Component
public class M01Catalogs {

    public static final SharedKind<CatalogOrigin> CARE_LEVEL =
            SharedKind.of("M01.CARE_LEVEL", CatalogOrigin.class).notDeletable();
    public static final SharedKind<CatalogOrigin> ALLERGY_SEVERITY =
            SharedKind.of("M01.ALLERGY_SEVERITY", CatalogOrigin.class).notDeletable();
    public static final SharedKind<CatalogOrigin> INFORMATION_SOURCE =
            SharedKind.of("M01.INFORMATION_SOURCE", CatalogOrigin.class).notDeletable();
    public static final SharedKind<CatalogOrigin> CARE_NEED =
            SharedKind.of("M01.CARE_NEED", CatalogOrigin.class).notDeletable();

    private final CatalogApi catalog;

    M01Catalogs(CatalogApi catalog) {
        this.catalog = catalog;
    }

    /** Mục có trong loại và đang Hiệu lực (chọn cho nghiệp vụ mới, FR-003). */
    public boolean isActive(SharedKind<CatalogOrigin> kind, long id) {
        try {
            return catalog.require(kind, id).status() == CatalogStatus.ACTIVE;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /** Các mục của loại theo id; id không có trong loại thì không có trong kết quả. */
    public List<CatalogItem<CatalogOrigin>> findAll(SharedKind<CatalogOrigin> kind, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Set<Long> wanted = Set.copyOf(ids);
        return catalog.list(kind, null, false).stream()
                .filter(i -> wanted.contains(i.id()))
                .toList();
    }

    public Map<Long, String> names(SharedKind<CatalogOrigin> kind, Collection<Long> ids) {
        return findAll(kind, ids).stream()
                .collect(Collectors.toMap(CatalogItem::id, CatalogItem::name, (a, b) -> a, LinkedHashMap::new));
    }
}
