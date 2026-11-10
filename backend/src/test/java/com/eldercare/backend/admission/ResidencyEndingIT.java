package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.ResidencyEndingCreated;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T057 (US7; FR-062, FR-066, FR-067; contracts/api-004.yaml, tag {@code residency-ending}): lập, xem, đổi ngày, hủy và
 * ghi nhận bàn giao của hồ sơ kết thúc lưu trú. Mỗi người tối đa một hồ sơ Đang chuẩn bị; Chuyển cơ sở bắt buộc nơi chuyển
 * đến; Bác sĩ chỉ lập Xuất viện, Chuyển cơ sở vì lý do y tế.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyEndingIT extends EndingTestBase {

    @Test
    void FR_062_createEnding_storesThePreparingRecord_callsFinalPeriod_andPublishesTheEvent() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(10);

        JsonNode created = JSON.readTree(createEndingRaw(office, endingBody(residentId, "DISCHARGE", expected))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREPARING"))
                .andExpect(jsonPath("$.endCase").value("DISCHARGE"))
                .andExpect(jsonPath("$.expectedEndDate").value(expected.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString());

        long id = created.get("id").asLong();
        Map<String, Object> row =
                jdbc.queryForMap("SELECT * FROM residency_ending WHERE kind = 'ENDING' AND id = ?", id);
        assertThat(row.get("status")).isEqualTo("PREPARING");
        assertThat(row.get("requested_by")).isEqualTo("Con gái ông A");
        assertThat(row.get("created_by")).isEqualTo(office.accountId());
        assertThat(finalPeriods.calls()).containsExactly("createDraft:" + residentId + ":" + expected);
        assertThat(row.get("final_period_ref"))
                .as("mã bảng kỳ cuối nháp do 010 trả")
                .isNotNull();
        assertThat(events.stream(ResidencyEndingCreated.class).filter(e -> e.residencyEndingId() == id))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ENDING_CREATED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(1);
        assertThat(residentStatus(residentId))
                .as("lập hồ sơ chưa đổi trạng thái")
                .isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    /** "Đổi api" 004 (A duyệt 2026-11-07): danh sách hồ sơ kết thúc lọc theo người cao tuổi và trạng thái. */
    @Test
    void FR_062_listEndings_filtersByResidentAndStatus() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(10));

        user.set(office.user());
        mvc.perform(get("/api/v1/residency-endings")
                        .param("residentId", String.valueOf(residentId))
                        .param("status", "PREPARING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("PREPARING"));
        mvc.perform(get("/api/v1/residency-endings")
                        .param("residentId", String.valueOf(residentId))
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
