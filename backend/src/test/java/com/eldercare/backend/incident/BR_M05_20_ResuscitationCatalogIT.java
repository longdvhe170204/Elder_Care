package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeView;
import com.eldercare.backend.resident.api.dto.WishChoice;
import org.junit.jupiter.api.Test;

/**
 * BR-M05-20, FR-048a: "biện pháp hồi sức" là loại diễn biến mang dấu trong danh mục (nhóm 1) do Quản lý viện cấu hình;
 * khởi tạo năm biện pháp; loại mới mang dấu cũng bị chặn khi chưa đối chiếu nguyện vọng.
 */
class BR_M05_20_ResuscitationCatalogIT extends EmergencyTestBase {

    @Test
    void BR_M05_20_fiveInitialResuscitationTypes() {
        assertThat(catalog.progressionTypes().stream()
                        .filter(ProgressionTypeView::resuscitation)
                        .map(ProgressionTypeView::code))
                .contains(
                        "CHEST_COMPRESSION",
                        "VENTILATION_SUPPORT",
                        "DEFIBRILLATION",
                        "INTUBATION",
                        "EMERGENCY_MEDICATION");
    }

    @Test
    void BR_M05_20_configuredResuscitationTypeIsAlsoBlocked() throws Exception {
        jdbc.update("""
                INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
                VALUES ('M05.PROGRESSION_TYPE', 'M05', 'TEST_RESUSCITATION', 'Biện pháp thử', 99, 'ACTIVE',
                  CAST('{"group":"ACTION","resuscitation":true}' AS JSON))
                """);
        long type = catalog.progressionTypes().stream()
                .filter(t -> t.code().equals("TEST_RESUSCITATION"))
                .findFirst()
                .orElseThrow()
                .id();
        activeWish(WishChoice.RETURN_HOME);
        long incident = criticalIncident();

        post(
                        nurse,
                        "/api/v1/incidents/{id}/progressions",
                        incident,
                        "{\"progressionTypeId\":" + type + ",\"content\":\"Thử\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WISH_NOT_CHECKED"));
    }
}
