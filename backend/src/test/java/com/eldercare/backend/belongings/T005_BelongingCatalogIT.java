package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * 013 T005 (FR-001, FR-001a, FR-002, FR-015a, FR-029; api-013 tag {@code belonging-catalogs}): danh mục loại đồ, mức
 * tình trạng, vị trí lưu giữ trên {@code catalog_item}.
 */
class T005_BelongingCatalogIT extends BelongingsTestBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private long id(String response) {
        return JSON.readTree(response).get("id").asLong();
    }

    @Test
    void FR_001_managerCreatesType_listShowsValuableFlagFromCfgM12_01() throws Exception {
        String name = unique("Máy tính bảng");
        long created = id(postJson(manager, "/api/v1/belongings/types", "{\"name\": \"" + name + "\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        getJson(office, "/api/v1/belongings/types")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + created + ")].valuable").value(false))
                .andExpect(
                        jsonPath("$[?(@.id == " + created + ")].hasCashAmount").value(false))
                .andExpect(jsonPath("$[?(@.name == 'Điện thoại')].valuable").value(true))
                .andExpect(jsonPath("$[?(@.name == 'Tiền mặt')].requiresConsentForUse")
                        .value(true));
    }

    @Test
    void FR_001_onlyManagerConfiguresTypes() throws Exception {
        postJson(office, "/api/v1/belongings/types", "{\"name\": \"" + unique("Đồng hồ") + "\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_015a_changingConsentFlag_requiresReason_andKeepsHistory() throws Exception {
        String name = unique("Ví");
        long typeId = id(postJson(manager, "/api/v1/belongings/types", "{\"name\": \"" + name + "\"}")
                .andReturn()
                .getResponse()
                .getContentAsString());

        putJson(
                        manager,
                        "/api/v1/belongings/types/" + typeId,
                        "{\"name\": \"" + name + "\", \"requiresConsentForUse\": true}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        putJson(
                        manager,
                        "/api/v1/belongings/types/" + typeId,
                        "{\"name\": \"" + name
                                + "\", \"requiresConsentForUse\": true, \"reason\": \"Ví thường có tiền\"}")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type = 'M12.BELONGING_TYPE'"
                                + " AND entity_id = ? AND attribute = 'requiresConsentForUse' AND value = 'true'"
                                + " AND reason = 'Ví thường có tiền'",
                        Integer.class,
                        typeId))
                .isEqualTo(1);
        getJson(manager, "/api/v1/belongings/types")
                .andExpect(jsonPath("$[?(@.id == " + typeId + ")].requiresConsentForUse")
                        .value(true));
    }

    @Test
    void FR_001_managerDeactivatesType() throws Exception {
        String name = unique("Gậy");
        long typeId = id(postJson(manager, "/api/v1/belongings/types", "{\"name\": \"" + name + "\"}")
                .andReturn()
                .getResponse()
                .getContentAsString());

        putJson(manager, "/api/v1/belongings/types/" + typeId, "{\"name\": \"" + name + "\", \"status\": \"INACTIVE\"}")
                .andExpect(status().isOk());

        getJson(manager, "/api/v1/belongings/types")
                .andExpect(jsonPath("$[?(@.id == " + typeId + ")].status").value("INACTIVE"));
    }

    @Test
    void FR_001a_managerAddsConditionLevel_secondUnusableLevelIsRejected() throws Exception {
        postJson(
                        manager,
                        "/api/v1/belongings/condition-levels",
                        "{\"name\": \"" + unique("Trầy nhẹ") + "\", \"order\": 2}")
                .andExpect(status().isCreated());
        postJson(
                        manager,
                        "/api/v1/belongings/condition-levels",
                        "{\"name\": \"" + unique("Hỏng hẳn") + "\", \"order\": 9, \"unusable\": true}")
                .andExpect(status().isConflict());
        postJson(office, "/api/v1/belongings/condition-levels", "{\"name\": \"" + unique("Mới") + "\", \"order\": 1}")
                .andExpect(status().isForbidden());

        getJson(nurse, "/api/v1/belongings/condition-levels")
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.name == 'Không dùng được')].unusable").value(true));
    }

    @Test
    void FR_002_officeStaffAndManagerSaveLocations_nurseCannot() throws Exception {
        long byOffice = id(
                postJson(office, "/api/v1/belongings/storage-locations", "{\"name\": \"" + unique("Tủ khóa HC") + "\"}")
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        postJson(
                        manager,
                        "/api/v1/belongings/storage-locations",
                        "{\"id\": " + byOffice + ", \"name\": \"" + unique("Tủ khóa HC mới")
                                + "\", \"description\": \"Phòng hành chính\", \"floorAreaId\": 2}")
                .andExpect(status().isOk());
        postJson(nurse, "/api/v1/belongings/storage-locations", "{\"name\": \"" + unique("Tủ tầng") + "\"}")
                .andExpect(status().isForbidden());

        getJson(nurse, "/api/v1/belongings/storage-locations")
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.id == " + byOffice + ")].description").value("Phòng hành chính"))
                .andExpect(
                        jsonPath("$[?(@.id == " + byOffice + ")].floorAreaId").value(2));
    }

    @Test
    void FR_002_locationWithHeldBelonging_cannotBeDeactivated() throws Exception {
        String name = unique("Tủ tầng 2");
        long location = id(postJson(office, "/api/v1/belongings/storage-locations", "{\"name\": \"" + name + "\"}")
                .andReturn()
                .getResponse()
                .getContentAsString());
        long belonging = heldBelonging(TestUsers.nextId(), location);

        postJson(
                        office,
                        "/api/v1/belongings/storage-locations",
                        "{\"id\": " + location + ", \"name\": \"" + name + "\", \"status\": \"INACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BELONGINGS_AT_LOCATION"));

        jdbc.update(
                "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?",
                belonging);
        postJson(
                        office,
                        "/api/v1/belongings/storage-locations",
                        "{\"id\": " + location + ", \"name\": \"" + name + "\", \"status\": \"INACTIVE\"}")
                .andExpect(status().isOk());
    }
}
