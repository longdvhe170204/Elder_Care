package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 019 T018 phần đính chính {@code M03.ASSET_HISTORY} (chủ dự án chốt 2026-11-07; 000 FR-026, FR-029): lần bảo trì và hư hỏng
 * đã xác nhận đính chính được ghi chú (nội dung, đơn vị, chi phí của lần bảo trì; mô tả hư hỏng) hoặc Hủy ghi nhận; không
 * đổi trạng thái, vị trí, bộ phận, hạn hiện hành của tài sản; chỉ Phụ trách bộ phận quản lý tài sản đính chính.
 */
class T018_AssetHistoryCorrectionIT extends AssetTestBase {
    static final String TYPE = "M03.ASSET_HISTORY";

    long maintained() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "start-maintenance", "{}").andExpect(status().isOk());
        command(
                        id,
                        "record-maintenance-result",
                        "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Thay lọc\","
                                + "\"performedBy\":\"Tổ kỹ thuật\",\"cost\":100000}")
                .andExpect(status().isOk());
        return id;
    }

    long eventOf(long asset, String kind) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM record_event WHERE entity_type='M03.ASSET' AND entity_id=? AND kind=?",
                Long.class,
                asset,
                kind);
    }

    ResultActions correct(long event, String body) throws Exception {
        return mvc.perform(post("/api/v1/corrections/" + TYPE + "/" + event)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String amend(String content) {
        return "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":" + content + ",\"reason\":\"Ghi nhầm\"}";
    }

    @Test
    void maintenanceNoteIsAmendedWithoutChangingTheAsset() throws Exception {
        long asset = maintained();
        long event = eventOf(asset, "M03.ASSET_MAINTENANCE");
        var before = jdbc.queryForMap("SELECT status, maintenance_due_on, floor_area_id FROM asset WHERE id=?", asset);
        correct(event, amend("{\"content\":\"Thay lọc và vệ sinh\",\"performedBy\":\"Công ty B\",\"cost\":120000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creationMethod").value("A"));
        mvc.perform(get("/api/v1/corrections/" + TYPE + "/" + event))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corrected").value(true))
                .andExpect(jsonPath("$.currentContent.content").value("Thay lọc và vệ sinh"))
                .andExpect(jsonPath("$.currentContent.cost").value(120000));
        assertThat(jdbc.queryForMap("SELECT status, maintenance_due_on, floor_area_id FROM asset WHERE id=?", asset))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='CORRECTION_ADDED' AND entity_id=?",
                        Integer.class,
                        Long.toString(event)))
                .isOne();
    }

    @Test
    void damageDescriptionIsAmendedAndOnlyTheOwnFieldsAreAccepted() throws Exception {
        long asset = created(donated("Tặng"));
        command(asset, "report-damage", "{\"description\":\"Gãy bánh xe\"}").andExpect(status().isOk());
        long event = eventOf(asset, "M03.ASSET_DAMAGE");
        correct(event, amend("{\"content\":\"không thuộc hư hỏng\"}")).andExpect(status().isUnprocessableEntity());
        correct(event, amend("{\"description\":\"Gãy hai bánh xe\",\"source\":\"BED_PREPARATION\"}"))
                .andExpect(status().isUnprocessableEntity());
        correct(event, amend("{}")).andExpect(status().isUnprocessableEntity());
        correct(event, amend("{\"description\":\"Gãy hai bánh xe\"}")).andExpect(status().isCreated());
        assertThat(stateOf(asset)).isEqualTo("DAMAGED");
    }

    @Test
    void maintenanceFieldsOutsideTheNoteAreRefused() throws Exception {
        long asset = maintained();
        long event = eventOf(asset, "M03.ASSET_MAINTENANCE");
        for (String field : new String[] {
            "\"result\":\"FAIL\"",
            "\"date\":\"2026-11-01\"",
            "\"outsourced\":true",
            "\"procurementRequestId\":1",
            "\"description\":\"x\"",
            "\"cost\":-1",
            "\"content\":\" \""
        }) correct(event, amend("{" + field + "}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void voidKeepsTheAssetAsItIs() throws Exception {
        long asset = maintained();
        long event = eventOf(asset, "M03.ASSET_MAINTENANCE");
        correct(event, "{\"type\":\"VOID\",\"reason\":\"Ghi nhầm tài sản\"}").andExpect(status().isCreated());
        assertThat(stateOf(asset)).isEqualTo("READY");
    }

    @Test
    void onlyTheDepartmentLeadCorrectsAndADraftCannotBeCorrected() throws Exception {
        long asset = maintained();
        long event = eventOf(asset, "M03.ASSET_MAINTENANCE");
        as(manager);
        correct(event, amend("{\"content\":\"x\"}")).andExpect(status().isForbidden());
        as(floorManager);
        correct(event, amend("{\"content\":\"x\"}")).andExpect(status().isForbidden());

        as(medicalLead);
        long open = created(donated("Tặng"));
        command(open, "start-maintenance", "{}").andExpect(status().isOk());
        correct(eventOf(open, "M03.ASSET_MAINTENANCE"), amend("{\"content\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECORD_NOT_CONFIRMED"));
    }
}
