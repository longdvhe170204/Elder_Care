package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.Gender;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T020, T022 (US2; FR-007, FR-008, FR-017, FR-018, BR-M02-15; contracts/api-004.yaml, tag {@code waiting-list}): đưa
 * vào danh sách chờ, xem, lọc, đánh dấu Ưu tiên (Q-394), xác nhận còn nhu cầu; Hành chính thao tác, Quản lý viện chỉ xem,
 * vai trò khác bị từ chối.
 */
@AutoConfigureMockMvc
@WithMockUser
class WaitingListEntryIT extends WaitingListTestBase {

    private int auditCount(String action, long entryId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.WAITING_LIST_ENTRY' AND entity_id = ?"
                        + " AND action = ?",
                Integer.class,
                String.valueOf(entryId),
                action);
    }

    @Test
    void FR_007_officeAddsApplicationToWaitingList_withoutPriority_andApplicationWaits() throws Exception {
        long residentId = admissionResident(Gender.FEMALE);
        long applicationId = application(residentId, today().minusDays(15), "SPECIAL");

        JsonNode entry = createEntry(applicationId);

        assertThat(entry.get("status").asString()).isEqualTo("WAITING");
        assertThat(entry.get("residentId").asLong()).isEqualTo(residentId);
        assertThat(entry.get("gender").asString()).isEqualTo("FEMALE");
        assertThat(entry.get("careLevel").asString()).isEqualTo("SPECIAL");
        assertThat(entry.get("notAssessed").asBoolean())
                .as("chưa có đánh giá: lấy mức dự kiến (FR-008)")
                .isTrue();
        assertThat(entry.get("priority").asBoolean())
                .as("hệ thống không tự đánh dấu (Q-307)")
                .isFalse();
        assertThat(entry.has("score")).as("không còn điểm (Q-394)").isFalse();
        assertThat(entry.get("applicationDate").asString())
                .isEqualTo(today().minusDays(15).toString());
        assertThat(entry.get("residenceType").asString()).isEqualTo("LONG_TERM_RESIDENTIAL");

        assertThat(jdbc.queryForObject(
                        "SELECT status FROM admission_record WHERE kind = 'APPLICATION' AND id = ?",
                        String.class,
                        applicationId))
                .isEqualTo("WAITING");
        long id = entry.get("id").asLong();
        assertThat(auditCount("M02_WAITING_ENTRY_CREATED", id)).isEqualTo(1);

        mvc.perform(get("/api/v1/admission-applications/" + applicationId))
                .andExpect(jsonPath("$.waitingListEntryId").value(id))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    private void markPriority(long id, boolean value, String reason) throws Exception {
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/mark-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":" + value + ",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    private static void insertOpenEntry(Statement statement, long applicationId, long residentId) throws SQLException {
        statement.executeUpdate(
                "INSERT INTO admission_record (kind, application_id, resident_id, care_level, care_level_not_assessed,"
                        + " last_confirmed_at, status, created_by, created_at) VALUES ('WAITING_ENTRY', "
                        + applicationId + ", " + residentId + ", 'BASIC', 1, NOW(6), 'WAITING', 1, NOW(6))");
    }
}
