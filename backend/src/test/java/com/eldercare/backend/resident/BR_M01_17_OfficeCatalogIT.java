package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.internal.health.service.CatalogService;
import com.eldercare.backend.resident.internal.health.service.CatalogService.Input;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BR-M01-17, Q-400, Phụ lục 27 dòng "Danh mục dị nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc" cột Hành chính
 * = X⁵⁰ (chủ dự án 2026-11-20): Hành chính xem được các mục Hiệu lực để chọn khi ghi tự khai (trước đó 403 nên form thêm
 * mục không mở được, BF-01 bước 3); không thấy mục Ngừng hiệu lực, không tạo danh mục.
 */
class BR_M01_17_OfficeCatalogIT extends HealthTestBase {

    @Autowired
    CatalogService catalogs;

    @Test
    void BR_M01_17_office_seesOnlyActiveCatalogItems_andCannotCreate() {
        String active = "Dị nguyên hiệu lực " + UUID.randomUUID();
        String inactive = "Dị nguyên ngừng " + UUID.randomUUID();
        insertAllergen(active, "ACTIVE");
        insertAllergen(inactive, "INACTIVE");

        user.set(office.user());
        var allergens = catalogs.list("allergens", null, Optional.empty());
        assertThat(allergens).extracting(i -> i.name()).contains(active).doesNotContain(inactive);
        assertThat(allergens).allMatch(i -> i.status() == CatalogStatus.ACTIVE);
        assertThat(catalogs.list("allergy-severities", null, Optional.of(CatalogStatus.ACTIVE)))
                .isNotEmpty();
        assertThat(catalogs.list("information-sources", null, Optional.of(CatalogStatus.ACTIVE)))
                .isNotEmpty();

        assertThatThrownBy(() -> catalogs.create("allergens", new Input("Dị nguyên mới", "FOOD", null)))
                .isInstanceOf(PermissionDeniedException.class);
    }

    private void insertAllergen(String name, String status) {
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes) VALUES"
                        + " ('M01.ALLERGEN', 'M01', ?, 0, ?, JSON_OBJECT('allergenGroup', 'FOOD'))",
                name,
                status);
    }
}
