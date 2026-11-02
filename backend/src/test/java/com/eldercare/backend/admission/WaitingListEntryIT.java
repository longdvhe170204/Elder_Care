package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.Gender;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.StreamSupport;
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

    @Test
    void FR_008_assessedResident_takesCurrentCareLevel() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");
        confirmedAssessment(residentId, CareLevel.SPECIAL);

        JsonNode entry = createEntry(applicationId);

        assertThat(entry.get("careLevel").asString()).isEqualTo("SPECIAL");
        assertThat(entry.get("notAssessed").asBoolean()).isFalse();
    }

    @Test
    void FR_007_secondEntryForTheSameApplication_isRejected409() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");
        createEntry(applicationId);

        mvc.perform(post("/api/v1/waiting-list-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(applicationId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_IN_PROGRESS"));
    }

    @Test
    void FR_007_unknownApplication_is404() throws Exception {
        mvc.perform(post("/api/v1/waiting-list-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(987_654_321L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_018_onlyOfficeOperates_managerViews_otherRolesDenied() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");

        user.set(manager.user());
        mvc.perform(post("/api/v1/waiting-list-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(applicationId)))
                .andExpect(status().isForbidden());

        long id = createEntry(applicationId).get("id").asLong();

        user.set(manager.user());
        mvc.perform(get("/api/v1/waiting-list-entries/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/waiting-list-entries")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/confirm-still-needed"))
                .andExpect(status().isForbidden());

        user.set(doctor.user());
        mvc.perform(get("/api/v1/waiting-list-entries")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/waiting-list-entries/" + id)).andExpect(status().isForbidden());

        user.set(nurse.user());
        mvc.perform(get("/api/v1/waiting-list-entries")).andExpect(status().isForbidden());
    }

    @Test
    void FR_018_viewingTheListIsNotAudited() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long id =
                createEntry(application(residentId, today(), "BASIC")).get("id").asLong();
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);

        mvc.perform(get("/api/v1/waiting-list-entries")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/waiting-list-entries/" + id)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class))
                .isEqualTo(before);
    }

    private void markPriority(long id, boolean value, String reason) throws Exception {
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/mark-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":" + value + ",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void FR_010_list_isPriorityFirst_thenApplicationDate_andFilteredByStatus() throws Exception {
        JsonNode closed = newEntry(Gender.FEMALE, today(), "BASIC");
        JsonNode later = newEntry(Gender.FEMALE, today().minusDays(1), "REGULAR");
        JsonNode earlier = newEntry(Gender.FEMALE, today().minusDays(2), "REGULAR");
        JsonNode marked = newEntry(Gender.FEMALE, today(), "SPECIAL");
        markPriority(marked.get("id").asLong(), true, "Vừa ra viện, sống một mình");
        jdbc.update(
                "UPDATE admission_record SET status = 'WAITING_CANCELLED' WHERE id = ?",
                closed.get("id").asLong());

        JsonNode all = JSON.readTree(mvc.perform(get("/api/v1/waiting-list-entries?status=WAITING"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        List<Long> ids = StreamSupport.stream(all.spliterator(), false)
                .map(e -> e.get("id").asLong())
                .toList();

        List<Long> mine = List.of(
                marked.get("id").asLong(),
                earlier.get("id").asLong(),
                later.get("id").asLong());
        assertThat(ids.stream().filter(mine::contains).toList())
                .as("Ưu tiên trước; rồi ngày đăng ký sớm hơn")
                .containsExactlyElementsOf(mine);
        assertThat(ids).doesNotContain(closed.get("id").asLong());
    }

    @Test
    void BR_M02_10_markingPriority_requiresReason_isAudited_andCanBeRemoved() throws Exception {
        long id = newEntry(Gender.MALE, today(), "REGULAR").get("id").asLong();

        markPriority(id, true, "Gia đình báo hoàn cảnh khó khăn");

        JsonNode marked = entry(id);
        assertThat(marked.get("priority").asBoolean()).isTrue();
        assertThat(marked.get("priorityReason").asString()).isEqualTo("Gia đình báo hoàn cảnh khó khăn");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.WAITING_LIST_ENTRY' AND entity_id = ?"
                                + " AND action = 'M02_WAITING_ENTRY_PRIORITY_MARKED' AND reason = ?"
                                + " AND data_before IS NOT NULL AND data_after IS NOT NULL",
                        Integer.class,
                        String.valueOf(id),
                        "Gia đình báo hoàn cảnh khó khăn"))
                .isEqualTo(1);

        markPriority(id, false, "Hoàn cảnh đã thay đổi");
        assertThat(entry(id).get("priority").asBoolean()).isFalse();
        assertThat(entry(id).has("priorityReason")).isFalse();

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/mark-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":true,\"reason\":\"  \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }

    @Test
    void BR_M02_10_markingPriority_isOfficeOnly() throws Exception {
        long id = newEntry(Gender.MALE, today(), "REGULAR").get("id").asLong();

        user.set(manager.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/mark-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":true,\"reason\":\"Quản lý viện thử\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_017_confirmStillNeeded_resetsTheUpdateMarkOnly() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        jdbc.update("UPDATE admission_record SET last_confirmed_at = ? WHERE id = ?", "2026-01-01 00:00:00", id);

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/confirm-still-needed"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject(
                        "SELECT last_confirmed_at > '2026-06-01' FROM admission_record WHERE kind = 'WAITING_ENTRY' AND id = ?",
                        Boolean.class,
                        id))
                .isTrue();
        assertThat(entry(id).get("status").asString()).isEqualTo("WAITING");
        assertThat(auditCount("M02_WAITING_ENTRY_CONFIRMED", id)).isEqualTo(1);
    }

    @Test
    void FR_017_confirmStillNeeded_onAClosedEntry_is409() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        jdbc.update("UPDATE admission_record SET status = 'WAITING_CANCELLED' WHERE id = ?", id);

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/confirm-still-needed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WAITING_ENTRY_NOT_WAITING"));
    }

    @Test
    void BR_M02_15_applicationDateIsLockedOnceTheEntryExists() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");
        createEntry(applicationId);
        String etag = mvc.perform(get("/api/v1/admission-applications/" + applicationId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        mvc.perform(post("/api/v1/admission-applications/" + applicationId + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBodyOn(residentId, today().minusDays(5), "BASIC")
                                .replace("}", ",\"reason\":\"Sửa ngày\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_DATE_LOCKED"));
    }

    @Test
    void FR_007_database_allowsOneOpenEntryPerResident_andContactedNeedsABed() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");
        long id = createEntry(applicationId).get("id").asLong();

        try (Connection connection = adminConnection();
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> insertOpenEntry(statement, applicationId, residentId))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uq_admission_record_open_entry");
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE admission_record SET status = 'CONTACTED' WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_admission_record_contacted_has_bed");
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE admission_record SET priority = TRUE, priority_reason = NULL WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_admission_record_priority");
            statement.executeUpdate(
                    "UPDATE admission_record SET status = 'DECLINED', locked_at = NOW(6) WHERE id = " + id);
            assertThatThrownBy(() ->
                            statement.executeUpdate("UPDATE admission_record SET status = 'WAITING' WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("row is locked");
        }
    }

    private static void insertOpenEntry(Statement statement, long applicationId, long residentId) throws SQLException {
        statement.executeUpdate(
                "INSERT INTO admission_record (kind, application_id, resident_id, care_level, care_level_not_assessed,"
                        + " last_confirmed_at, status, created_by, created_at) VALUES ('WAITING_ENTRY', "
                        + applicationId + ", " + residentId + ", 'BASIC', 1, NOW(6), 'WAITING', 1, NOW(6))");
    }
}
