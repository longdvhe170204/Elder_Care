package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Gender;
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
 * vào danh sách chờ, xem, lọc, sửa tình huống đặc biệt, xác nhận còn nhu cầu; Hành chính thao tác, Quản lý viện chỉ xem,
 * vai trò khác bị từ chối.
 */
@AutoConfigureMockMvc
@WithMockUser
class WaitingListEntryIT extends WaitingListTestBase {

    private static final String CIRCUMSTANCE_REASON = "Gia đình báo thay đổi hoàn cảnh";

    private int scoreOf(JsonNode entry) {
        return entry.get("score").asInt();
    }

    private static List<String> codes(JsonNode entry) {
        return StreamSupport.stream(entry.get("circumstances").spliterator(), false)
                .map(c -> c.get("code").asString())
                .sorted()
                .toList();
    }

    private int auditCount(String action, long entryId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.WAITING_LIST_ENTRY' AND entity_id = ?"
                        + " AND action = ?",
                Integer.class,
                String.valueOf(entryId),
                action);
    }

    private String updateCircumstances(long entryId, String body) throws Exception {
        return mvc.perform(post("/api/v1/waiting-list-entries/" + entryId + "/update-circumstances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void FR_007_officeAddsApplicationToWaitingList_scoredAndApplicationWaits() throws Exception {
        setScoreTable(SCORE_TABLE);
        long residentId = admissionResident(Gender.FEMALE);
        long applicationId = application(residentId, today().minusDays(15), "SPECIAL");

        JsonNode entry = createEntry(applicationId, "LIVING_ALONE");

        assertThat(entry.get("status").asString()).isEqualTo("WAITING");
        assertThat(entry.get("residentId").asLong()).isEqualTo(residentId);
        assertThat(entry.get("gender").asString()).isEqualTo("FEMALE");
        assertThat(entry.get("careLevel").asString()).isEqualTo("SPECIAL");
        assertThat(entry.get("notAssessed").asBoolean())
                .as("chưa có đánh giá: lấy mức dự kiến (FR-008)")
                .isTrue();
        assertThat(scoreOf(entry)).as("5 + 2 + 2 (US2 #1)").isEqualTo(9);
        assertThat(entry.get("scoreBreakdown").get("careLevel").asInt()).isEqualTo(5);
        assertThat(entry.get("scoreBreakdown").get("waitingTime").asInt()).isEqualTo(2);
        assertThat(entry.get("scoreBreakdown").get("circumstances").asInt()).isEqualTo(2);
        assertThat(entry.get("scoreBreakdown").get("adjustment").asInt()).isZero();
        assertThat(codes(entry)).containsExactly("LIVING_ALONE");
        assertThat(entry.get("circumstances").get(0).get("status").asString()).isEqualTo("CONFIRMED");
        assertThat(entry.get("applicationDate").asString())
                .isEqualTo(today().minusDays(15).toString());
        assertThat(entry.get("residenceType").asString()).isEqualTo("LONG_TERM_RESIDENTIAL");

        assertThat(jdbc.queryForObject(
                        "SELECT status FROM admission_application WHERE id = ?", String.class, applicationId))
                .isEqualTo("WAITING");
        long id = entry.get("id").asLong();
        assertThat(auditCount("M02_WAITING_ENTRY_CREATED", id)).isEqualTo(1);
        JsonNode history = scoreHistory(id);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("cause").asString()).isEqualTo("CREATED");
        assertThat(history.get(0).get("oldScore").asInt()).isZero();
        assertThat(history.get(0).get("newScore").asInt()).isEqualTo(9);

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
    void FR_007_unknownCircumstance_is422() throws Exception {
        setScoreTable(SCORE_TABLE);
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");

        mvc.perform(post("/api/v1/waiting-list-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(applicationId, "NOT_IN_TABLE")))
                .andExpect(status().isUnprocessableContent());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM waiting_list_entry WHERE application_id = ?",
                        Integer.class,
                        applicationId))
                .isZero();
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
        mvc.perform(get("/api/v1/waiting-list-entries/" + id + "/score-history"))
                .andExpect(status().isForbidden());

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

    @Test
    void FR_009_list_isSortedByScoreThenApplicationDateThenCreation_andFilteredByStatus() throws Exception {
        setScoreTable(SCORE_TABLE);
        JsonNode low = newEntry(Gender.FEMALE, today(), "BASIC");
        JsonNode tiedLater = newEntry(Gender.FEMALE, today().minusDays(1), "REGULAR");
        JsonNode tiedEarlier = newEntry(Gender.FEMALE, today().minusDays(2), "REGULAR");
        JsonNode high = newEntry(Gender.FEMALE, today(), "SPECIAL");
        jdbc.update(
                "UPDATE waiting_list_entry SET status = 'WAITING_CANCELLED' WHERE id = ?",
                low.get("id").asLong());

        JsonNode all = JSON.readTree(mvc.perform(get("/api/v1/waiting-list-entries?status=WAITING"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        List<Long> ids = StreamSupport.stream(all.spliterator(), false)
                .map(e -> e.get("id").asLong())
                .toList();

        List<Long> mine = List.of(
                high.get("id").asLong(),
                tiedEarlier.get("id").asLong(),
                tiedLater.get("id").asLong());
        assertThat(ids.stream().filter(mine::contains).toList())
                .as("điểm giảm dần; hòa điểm thì ngày đăng ký sớm hơn trước")
                .containsExactlyElementsOf(List.of(
                        high.get("id").asLong(),
                        tiedEarlier.get("id").asLong(),
                        tiedLater.get("id").asLong()));
        assertThat(ids).doesNotContain(low.get("id").asLong());
    }

    @Test
    void FR_007_updateCircumstances_rescoresAndRecordsHistoryAndAudit() throws Exception {
        setScoreTable(SCORE_TABLE);
        JsonNode entry = newEntry(Gender.MALE, today(), "REGULAR", "LIVING_ALONE");
        long id = entry.get("id").asLong();
        assertThat(scoreOf(entry)).isEqualTo(2 + 2);

        updateCircumstances(
                id,
                "{\"circumstances\":[\"LIVING_ALONE\",\"JUST_DISCHARGED\"],\"reason\":\"" + CIRCUMSTANCE_REASON
                        + "\"}");

        JsonNode after = entry(id);
        assertThat(codes(after)).containsExactly("JUST_DISCHARGED", "LIVING_ALONE");
        assertThat(scoreOf(after)).isEqualTo(2 + 2 + 3);
        JsonNode history = scoreHistory(id);
        assertThat(history.get(history.size() - 1).get("cause").asString()).isEqualTo("CIRCUMSTANCES_CHANGED");
        assertThat(history.get(history.size() - 1).get("oldScore").asInt()).isEqualTo(4);
        assertThat(history.get(history.size() - 1).get("newScore").asInt()).isEqualTo(7);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.WAITING_LIST_ENTRY' AND entity_id = ?"
                                + " AND action = 'M02_WAITING_ENTRY_CIRCUMSTANCES_UPDATED' AND reason = ?"
                                + " AND data_before IS NOT NULL AND data_after IS NOT NULL",
                        Integer.class,
                        String.valueOf(id),
                        CIRCUMSTANCE_REASON))
                .isEqualTo(1);

        updateCircumstances(id, "{\"circumstances\":[],\"reason\":\"" + CIRCUMSTANCE_REASON + "\"}");
        assertThat(scoreOf(entry(id))).isEqualTo(2);
    }

    @Test
    void FR_007_updateCircumstances_withoutReason_is422_andUnknownCode_is422() throws Exception {
        setScoreTable(SCORE_TABLE);
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/update-circumstances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"circumstances\":[\"LIVING_ALONE\"],\"reason\":\"  \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/update-circumstances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"circumstances\":[\"NOT_IN_TABLE\"],\"reason\":\"" + CIRCUMSTANCE_REASON + "\"}"))
                .andExpect(status().isUnprocessableContent());

        assertThat(codes(entry(id))).isEmpty();
    }

    @Test
    void FR_007_pendingCircumstance_countsOnlyOnceConfirmed() throws Exception {
        setScoreTable(SCORE_TABLE);
        JsonNode entry = newEntry(Gender.MALE, today(), "BASIC");
        long id = entry.get("id").asLong();
        // "Từng lưu trú tại viện" được đánh dấu sẵn ở trạng thái chờ xác nhận (FR-007): dựng thẳng ở cơ sở dữ liệu
        jdbc.update(
                "INSERT INTO waiting_list_circumstance (entry_id, circumstance_code, status) VALUES (?, 'LIVING_ALONE',"
                        + " 'PENDING_CONFIRMATION')",
                id);
        assertThat(entry(id).get("circumstances").get(0).get("status").asString())
                .isEqualTo("PENDING_CONFIRMATION");

        updateCircumstances(id, "{\"circumstances\":[\"LIVING_ALONE\"],\"reason\":\"" + CIRCUMSTANCE_REASON + "\"}");

        JsonNode after = entry(id);
        assertThat(after.get("circumstances").get(0).get("status").asString()).isEqualTo("CONFIRMED");
        assertThat(scoreOf(after)).isEqualTo(2);
    }

    @Test
    void FR_017_confirmStillNeeded_resetsTheUpdateMarkOnly() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        jdbc.update("UPDATE waiting_list_entry SET last_confirmed_at = ? WHERE id = ?", "2026-01-01 00:00:00", id);

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/confirm-still-needed"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject(
                        "SELECT last_confirmed_at > '2026-06-01' FROM waiting_list_entry WHERE id = ?",
                        Boolean.class,
                        id))
                .isTrue();
        assertThat(entry(id).get("status").asString()).isEqualTo("WAITING");
        assertThat(auditCount("M02_WAITING_ENTRY_CONFIRMED", id)).isEqualTo(1);
    }

    @Test
    void FR_017_confirmStillNeeded_onAClosedEntry_is409() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        jdbc.update("UPDATE waiting_list_entry SET status = 'WAITING_CANCELLED' WHERE id = ?", id);

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
                    .hasMessageContaining("uq_waiting_list_entry_open");
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE waiting_list_entry SET status = 'CONTACTED' WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_waiting_list_entry_contacted_has_bed");
            statement.executeUpdate("UPDATE waiting_list_entry SET status = 'DECLINED' WHERE id = " + id);
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE waiting_list_entry SET status = 'WAITING' WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("closed waiting list entry");
        }
    }

    private static void insertOpenEntry(Statement statement, long applicationId, long residentId) throws SQLException {
        statement.executeUpdate(
                "INSERT INTO waiting_list_entry (application_id, resident_id, care_level, care_level_not_assessed,"
                        + " current_score, score_breakdown, last_confirmed_at, created_by, created_at) VALUES ("
                        + applicationId + ", " + residentId + ", 'BASIC', 1, 0, '{}', NOW(6), 1, NOW(6))");
    }
}
