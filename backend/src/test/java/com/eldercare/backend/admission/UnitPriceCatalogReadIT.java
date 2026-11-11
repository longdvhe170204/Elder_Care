package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Đổi api 004 (A duyệt 2026-11-10, 010 Q1, Q2, Q4): 010 đọc loại, đơn vị, trạng thái của dịch vụ theo mã nguồn (kể cả mục
 * Ngừng hiệu lực), tra tên danh mục để chặn khoản nhập tay trùng danh mục (010 FR-021) và lấy dịch vụ Hiệu lực theo loại
 * cho nguồn không mang mã dịch vụ.
 */
class UnitPriceCatalogReadIT extends ContractTestBase {

    @Autowired
    UnitPriceApi prices;

    private long service(String name, String type, String unit, Long medicationId) {
        jdbc.update(M02Sql.INSERT_SERVICE, name, M02Sql.attributes(type, unit, "OCCURRENCE", null, null, medicationId));
        return jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
    }

    @Test
    void service_readsInactiveItem_withTypeUnitAndMedication() {
        String name = "Tã người lớn " + UUID.randomUUID();
        long diaper = service(name, "CONSUMABLE", "gói", null);
        jdbc.update("UPDATE catalog_item SET status = 'INACTIVE' WHERE id = ?", diaper);
        long medicine = service("Thuốc viện " + UUID.randomUUID(), "FACILITY_SUPPLIED_MEDICATION", "viên", 77L);

        ServiceView found = prices.service(diaper).orElseThrow();
        assertThat(found.type()).isEqualTo(ServiceType.CONSUMABLE);
        assertThat(found.unit()).isEqualTo("gói");
        assertThat(found.active()).isFalse();
        assertThat(prices.service(medicine).orElseThrow().medicationId()).isEqualTo(77L);
        assertThat(prices.service(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void servicesNamed_matchesIgnoringCaseAndSpaces_includingInactive_butKeepsAccents() {
        String suffix = UUID.randomUUID().toString();
        long gauze = service("Gạc vô trùng " + suffix, "MEDICAL_SUPPLY", "gói", null);
        jdbc.update("UPDATE catalog_item SET status = 'INACTIVE' WHERE id = ?", gauze);

        assertThat(prices.servicesNamed("  gạc   VÔ trùng " + suffix.toUpperCase() + " "))
                .extracting(ServiceView::id)
                .containsExactly(gauze);
        assertThat(prices.servicesNamed("Gac vo trung " + suffix)).isEmpty();
    }

    @Test
    void activeServicesOf_returnsOnlyActiveOfType() {
        long stay = service("Người thân ở lại " + UUID.randomUUID(), "FAMILY_STAY", "đêm", null);
        long old = service("Người thân ở lại cũ " + UUID.randomUUID(), "FAMILY_STAY", "đêm", null);
        jdbc.update("UPDATE catalog_item SET status = 'INACTIVE' WHERE id = ?", old);

        assertThat(prices.activeServicesOf(ServiceType.FAMILY_STAY))
                .extracting(ServiceView::id)
                .contains(stay)
                .doesNotContain(old);
    }
}
