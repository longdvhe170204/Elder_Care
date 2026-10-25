package com.cambae.backend.m02;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.support.TestUsers.TestUser;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nền chung cho test danh sách chờ của 004 (US2; FR-007 → FR-018): bảng điểm CFG-M02-08 do Quản lý viện đặt qua API
 * tham số rồi khôi phục mặc định sau mỗi test; hồ sơ chờ lập qua HTTP. Lớp con khai {@code @AutoConfigureMockMvc} và
 * {@code @WithMockUser}.
 */
abstract class WaitingListTestBase extends M02TestBase {

    protected static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicLong NAMES = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    /** Điểm: Cơ bản 0, Thường xuyên 2, Đặc biệt 5; sống một mình 2, vừa ra viện 3; mỗi 7 ngày chờ +1 (US2 #1). */
    protected static final String SCORE_TABLE = """
            {"waitingPeriod":{"value":7,"unit":"DAYS"},"pointsPerWaitingPeriod":1,
             "careLevelPoints":{"BASIC":0,"REGULAR":2,"SPECIAL":5},
             "specialSituationPoints":{"LIVING_ALONE":2,"JUST_DISCHARGED":3}}""";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected TestClockControl clock;

    protected TestUser manager;
    private boolean tableChanged;

    @BeforeEach
    void waitingListUsers() throws Exception {
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        clock.reset();
        cleanDailyState();
    }

    @AfterEach
    void restoreScoreTable() throws Exception {
        clock.reset();
        if (tableChanged) {
            tableChanged = false;
            user.set(manager.user());
            String etag = mvc.perform(get("/api/v1/parameters/CFG-M02-08"))
                    .andReturn()
                    .getResponse()
                    .getHeader("ETag");
            mvc.perform(post("/api/v1/parameters/CFG-M02-08/change-value")
                            .with(csrf())
                            .header("If-Match", etag)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"restoreDefault\":true,\"reason\":\"khôi phục sau test\"}"))
                    .andExpect(status().isOk());
        }
        cleanDailyState();
    }

    /** Việc tính điểm, nhắc dùng chung bảng {@code daily_run}/{@code m02_reminder} nên dọn trước và sau mỗi test. */
    protected void cleanDailyState() throws SQLException {
        admin(
                "DELETE FROM daily_run WHERE task IN ('CALCULATE_SCORES', 'REMINDERS')",
                "DELETE FROM m02_reminder WHERE type = 'WAITING_ENTRY_UPDATE'");
    }

    /** Chạy lệnh bằng tài khoản quản trị: tài khoản ứng dụng không có DELETE ở bảng kỹ thuật. */
    protected static void admin(String... statements) throws SQLException {
        try (Connection connection = adminConnection();
                Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.executeUpdate(sql);
            }
        }
    }

    /** Quản lý viện đặt bảng CFG-M02-08; người dùng hiện tại trở lại là Hành chính. */
    protected void setScoreTable(String valueJson) throws Exception {
        user.set(manager.user());
        String etag = mvc.perform(get("/api/v1/parameters/CFG-M02-08"))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/parameters/CFG-M02-08/change-value")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":" + valueJson + ",\"reason\":\"Đặt bảng điểm cho test\"}"))
                .andExpect(status().isOk());
        tableChanged = true;
        user.set(office.user());
    }

    /** Hồ sơ Đang tiếp nhận có giới tính cho trước. */
    protected long admissionResident(Gender gender) {
        user.set(office.user());
        long id = profiles.create(
                        new PersonalInfo(
                                "Nguyễn Văn Chờ " + NAMES.incrementAndGet(),
                                LocalDate.of(1944, 5, 3),
                                gender,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        false)
                .profile()
                .id();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        return id;
    }

    protected String applicationBodyOn(long residentId, LocalDate applicationDate, String careLevel) {
        return """
                {"residentId":%d,"applicationDate":"%s","contactPerson":"Người liên hệ",
                 "needs":"Cần chăm sóc","desiredResidenceType":"LONG_TERM_RESIDENTIAL",
                 "expectedCareLevel":"%s","desiredAdmissionDate":"%s","note":"Ghi chú"}""".formatted(residentId, applicationDate, careLevel, applicationDate.plusDays(30));
    }

    /** Lượt đăng ký Đang xử lý ngày {@code applicationDate}, mức dự kiến {@code careLevel}; trả mã lượt. */
    protected long application(long residentId, LocalDate applicationDate, String careLevel) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBodyOn(residentId, applicationDate, careLevel)))
                .andExpect(status().isCreated());
        return applicationIdOf(residentId);
    }

    protected String entryBody(long applicationId, String... circumstances) {
        return "{\"applicationId\":" + applicationId + ",\"circumstances\":["
                + Stream.of(circumstances).map(c -> "\"" + c + "\"").collect(Collectors.joining(",")) + "]}";
    }

    /** Đưa lượt vào danh sách chờ với tư cách Hành chính; trả hồ sơ chờ. */
    protected JsonNode createEntry(long applicationId, String... circumstances) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(post("/api/v1/waiting-list-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(applicationId, circumstances)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    /** Hồ sơ chờ mới của một người mới: đăng ký ngày {@code applicationDate}, mức dự kiến {@code careLevel}. */
    protected JsonNode newEntry(Gender gender, LocalDate applicationDate, String careLevel, String... circumstances)
            throws Exception {
        long residentId = admissionResident(gender);
        long applicationId = application(residentId, applicationDate, careLevel);
        return createEntry(applicationId, circumstances);
    }

    protected JsonNode entry(long id) throws Exception {
        return JSON.readTree(mvc.perform(get("/api/v1/waiting-list-entries/" + id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    protected JsonNode scoreHistory(long id) throws Exception {
        return JSON.readTree(mvc.perform(get("/api/v1/waiting-list-entries/" + id + "/score-history"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    /** Đặt đồng hồ giả lập vào 12:00 ngày {@code day} (sau 00:15 nên job hằng ngày được phép chạy). */
    protected void noonOf(LocalDate day) {
        clock.setTo(day.atTime(12, 0).atZone(BusinessTime.ZONE).toInstant());
    }
}
