package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

/**
 * 019 T016 lượt 1 (US4 kịch bản 1 → 3, 7, 8; FR-008c, FR-011 → FR-013, FR-021; BR-M03-18, DBR-36, Q-284, Q-292, Q-294,
 * Q-313; quickstart Q18): ghi tăng, đưa vào sử dụng, chuyển vị trí, báo hỏng theo tầng, bảo trì và kết quả, chuyển bộ
 * phận, danh mục loại có hai loại hệ thống Giường, Xe.
 */
class FR_013_AssetLifecycleIT extends AssetTestBase {
    /** FR-011: danh mục loại để màn web hiện tên loại, chọn loại khi ghi tăng, lọc xe (GET /asset-types, 2026-11-10). */
    @Test
    void FR_011_assetTypesAreListedWithSystemCodes() throws Exception {
        mvc.perform(get("/api/v1/asset-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + bedType + ")].code").value("BED"))
                .andExpect(jsonPath("$[?(@.code=='VEHICLE')].status").value("ACTIVE"))
                .andExpect(jsonPath("$[?(@.id==" + equipmentType + ")].status").value("ACTIVE"));
    }

    @Test
    void Q18_Q294_purchasedAssetNeedsApprovedRequestOfSameDepartmentAndAcquisitionCost() throws Exception {
        long request = approvedRequest(medical, "ASSET");
        create(purchased(equipmentType, request, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ACQUISITION_COST_REQUIRED"));
        create(purchased(equipmentType, null, 1_500_000L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROCUREMENT_REQUEST_REQUIRED"));
        create(purchased(equipmentType, approvedRequest(care, "ASSET"), 1_500_000L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROCUREMENT_REQUEST_NOT_USABLE"));

        long id = created(purchased(equipmentType, request, 1_500_000L));
        assertThat(jdbc.queryForMap("SELECT status, location_type, acquisition_cost FROM asset WHERE id=?", id))
                .containsEntry("status", "READY")
                .containsEntry("location_type", "ASSET_STORAGE");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_CONTAINS(linked_sources, JSON_OBJECT('sourceType','ASSET','sourceId',?))"
                                + " FROM procurement_request WHERE id=?",
                        Integer.class,
                        id,
                        request))
                .isOne();

        create(donated(null)).andExpect(status().isUnprocessableEntity());
        long donatedId = created(donated("Hội người cao tuổi phường tặng"));
        assertThat(stateOf(donatedId)).isEqualTo("READY");
    }
}
