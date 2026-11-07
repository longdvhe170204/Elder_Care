package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.api.events.AssetStatusChanged;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 019 T016 lượt 1 (US4 kịch bản 1 → 3, 7, 8; FR-008c, FR-011 → FR-013, FR-021; BR-M03-18, DBR-36, Q-284, Q-292, Q-294,
 * Q-313; quickstart Q18): ghi tăng, đưa vào sử dụng, chuyển vị trí, báo hỏng theo tầng, bảo trì và kết quả, chuyển bộ
 * phận, danh mục loại có hai loại hệ thống Giường, Xe.
 */
class FR_013_AssetLifecycleIT extends AssetTestBase {
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

    @Test
    void Q238_Q292_bedTypeIsNotRecordedHereAndOnlyTheDepartmentLeadRecords() throws Exception {
        create(donated("Tặng").replace("\"typeId\":" + equipmentType, "\"typeId\":" + bedType))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BED_ASSET_TYPE_NOT_ALLOWED"));
        as(manager);
        create(donated("Tặng")).andExpect(status().isForbidden());
        as(careLead);
        create(donated("Tặng"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_DEPARTMENT_LEAD"));
    }

    @Test
    void US4_1_2_putIntoUseThenFloorManagerOfThatFloorReportsDamage() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "put-into-use", "{\"location\":{\"type\":\"ROOM\",\"id\":" + room + "}}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT status, floor_area_id FROM asset WHERE id=?", id))
                .containsEntry("status", "IN_USE")
                .containsEntry("floor_area_id", floor);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type='M03.ASSET' AND entity_id=?"
                                + " AND attribute='LOCATION'",
                        Integer.class,
                        id))
                .isEqualTo(2);
        mvc.perform(get(URL).param("departmentId", Long.toString(medical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].location").value("Phòng 203"));

        as(otherFloorManager);
        command(id, "report-damage", "{\"description\":\"Gãy bánh xe\"}").andExpect(status().is4xxClientError());
        as(floorManager);
        command(id, "report-damage", "{}").andExpect(status().isUnprocessableEntity());
        command(id, "report-damage", "{\"description\":\"Gãy bánh xe\"}").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("DAMAGED");
        assertThat(events.stream(AssetStatusChanged.class)
                        .filter(e -> e.assetId() == id && e.to().name().equals("DAMAGED"))
                        .flatMap(e -> e.notificationRequests().stream())
                        .flatMap(n -> n.recipientGroups().stream()))
                .containsExactly(new RecipientGroup.DepartmentLead(medical, new Substitution.Default()));
        command(id, "report-damage", "{\"description\":\"Lần nữa\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void US4_3_maintenanceResultAndOutsourcedCostNeedsAnApprovedServiceRequest() throws Exception {
        long id = created(purchased(equipmentType, approvedRequest(medical, "ASSET"), 1_500_000L));
        as(floorManager);
        command(id, "start-maintenance", "{}")
                .andExpect(status().is4xxClientError()); // ở kho tài sản, không thuộc tầng
        as(medicalLead);
        command(id, "start-maintenance", "{}").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("UNDER_MAINTENANCE");

        command(
                        id,
                        "record-maintenance-result",
                        "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Thay lọc\"}")
                .andExpect(status().isUnprocessableEntity());
        String outsourced = "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Thay lọc\","
                + "\"performedBy\":\"Công ty A\",\"outsourced\":true,\"cost\":300000";
        command(id, "record-maintenance-result", outsourced + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROCUREMENT_REQUEST_REQUIRED"));
        long service = approvedRequest(medical, "SERVICE");
        command(id, "record-maintenance-result", outsourced + ",\"procurementRequestId\":" + service + "}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT status, maintenance_due_on FROM asset WHERE id=?", id))
                .containsEntry("status", "READY")
                .containsEntry("maintenance_due_on", java.sql.Date.valueOf("2027-02-04"));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(linked_sources) FROM procurement_request WHERE id=?",
                        Integer.class,
                        service))
                .isOne();

        command(id, "start-maintenance", "{}").andExpect(status().isOk());
        command(
                        id,
                        "record-maintenance-result",
                        "{\"result\":\"FAIL\",\"date\":\"2026-11-06\",\"content\":\"Hỏng mô tơ\",\"performedBy\":\"Tổ kỹ thuật\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("DAMAGED");
    }

    @Test
    void US4_7_onlyTheManagerTransfersDepartmentWithAReason() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "transfer-department", "{\"newDepartmentId\":" + care + ",\"reason\":\"Chuyển\"}")
                .andExpect(status().isForbidden());
        as(manager);
        command(id, "transfer-department", "{\"newDepartmentId\":" + care + "}")
                .andExpect(status().isUnprocessableEntity());
        command(id, "transfer-department", "{\"newDepartmentId\":" + care + ",\"reason\":\"Bộ phận Chăm sóc dùng\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT department_id FROM asset WHERE id=?", Long.class, id))
                .isEqualTo(care);
        assertThat(jdbc.queryForMap(
                        "SELECT value_before, value FROM status_history WHERE entity_type='M03.ASSET' AND entity_id=?"
                                + " AND attribute='DEPARTMENT'",
                        id))
                .containsEntry("value_before", Long.toString(medical))
                .containsEntry("value", Long.toString(care));
    }

    @Test
    void FR_011_descriptionIsEditedWithIfMatchAndTypeCatalogIsManagedByManager() throws Exception {
        long id = created(donated("Tặng"));
        String edit = donated("Tặng").replace("\"name\":\"Xe lăn\"", "\"name\":\"Xe lăn tay\"");
        mvc.perform(put(URL + "/" + id)
                        .header("Idempotency-Key", "ap-" + TestUsers.nextId())
                        .header("If-Match", "\"7\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edit))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(put(URL + "/" + id)
                        .header("Idempotency-Key", "ap-" + TestUsers.nextId())
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edit))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT name FROM asset WHERE id=?", String.class, id))
                .isEqualTo("Xe lăn tay");

        mvc.perform(post("/api/v1/asset-types")
                        .header("Idempotency-Key", "at-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nội thất\"}"))
                .andExpect(status().isForbidden());
        as(manager);
        mvc.perform(post("/api/v1/asset-types")
                        .header("Idempotency-Key", "at-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + bedType + ",\"name\":\"Giường\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isConflict());
    }
}
