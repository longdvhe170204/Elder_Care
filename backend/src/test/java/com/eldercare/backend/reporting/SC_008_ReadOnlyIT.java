package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.TestScopedMetrics.Event;
import com.eldercare.backend.reporting.TestScopedMetrics.EventSource;
import com.eldercare.backend.reporting.internal.report.domain.ReportGroup;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SC-008, FR-001, US7 #3 (016 T027, {@code SC_008_ChiDocIT}; quickstart Q29): mọi thao tác xem không
 * tạo, sửa, đổi trạng thái bản ghi nào, so bằng checksum từng bảng toàn schema; chỉ lần xuất thành công thêm đúng một
 * bản ghi "Xuất báo cáo" và một tệp. Bỏ {@code login_session}, {@code device} vì mỗi yêu cầu ghi lần hoạt động cuối (không phải
 * dữ liệu nghiệp vụ).
 */
@AutoConfigureMockMvc
@Import({TestScopedMetrics.class, FakeFileConfig.class})
class SC_008_ReadOnlyIT extends MySqlIntegrationTestBase {

    private static final Set<String> NOT_BUSINESS = Set.of("login_session", "device");

    private static final Set<String> EXPORT_WRITES = Set.of("audit_log", "file", "idempotency_key");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    EventSource testHealth;

    @Autowired
    EventSource testCosts;

    TestUsers users;
    String manager;
    String office;
    String floorManager;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        manager = token(users.staff(Role.NURSING_HOME_MANAGER));
        office = token(users.staff(Role.OFFICE_STAFF));
        floorManager = token(users.staff(Role.FLOOR_MANAGER));
        var at = time.now().minus(Duration.ofDays(1));
        for (EventSource source : List.of(testHealth, testCosts)) {
            source.events.add(new Event("e-" + TestUsers.nextId(), at, TestUsers.nextId(), TestUsers.nextId()));
        }
    }

    @AfterEach
    void reset() {
        testHealth.events.clear();
        testCosts.events.clear();
    }

    private String token(TestUser user) {
        return TestUsers.bearer(users.session(user.accountId()));
    }

    /** Bảng → checksum, mọi bảng của schema test ({@link #checksum}). */
    private Map<String, Long> snapshot() {
        Map<String, Long> result = new TreeMap<>();
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
                        + " AND table_type = 'BASE TABLE' AND table_name NOT LIKE 'flyway%'",
                String.class);
        for (String table : tables) {
            if (!NOT_BUSINESS.contains(table)) {
                result.put(table, checksum(table));
            }
        }
        return result;
    }

    /**
     * Số dòng và tổng CRC32 của mọi cột thường. Không dùng {@code CHECKSUM TABLE}: với bảng có chỉ mục đa giá trị trên
     * JSON ({@code family_medication_batch} của 006, cột ảo ẩn) MySQL 8.0 trả giá trị khác nhau ở mỗi kết nối dù không có
     * lệnh ghi nào (2026-11-03).
     */
    private long checksum(String table) {
        String row = jdbc
                .queryForList(
                        "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE()"
                                + " AND table_name = ? ORDER BY ordinal_position",
                        String.class,
                        table)
                .stream()
                .map(column -> "IFNULL(CAST(`" + column + "` AS CHAR), '~')")
                .collect(Collectors.joining(", '|', "));
        return jdbc.queryForObject(
                "SELECT COUNT(*) * 4294967296 + COALESCE(SUM(CRC32(CONCAT(" + row + "))), 0) FROM `" + table + "`",
                Long.class);
    }

    private void viewEverything(String token) throws Exception {
        LocalDate today = time.today();
        mvc.perform(get("/api/v1/dashboard").header("Authorization", token));
        mvc.perform(get("/api/v1/metrics").header("Authorization", token));
        for (ReportGroup group : ReportGroup.values()) {
            mvc.perform(get("/api/v1/reports/" + group)
                    .param("from", today.minusDays(7).toString())
                    .param("to", today.toString())
                    .header("Authorization", token));
        }
        for (String metric : List.of(TestScopedMetrics.HEALTH, TestScopedMetrics.COSTS)) {
            mvc.perform(get("/api/v1/metric-details/" + metric)
                    .param("from", today.minusDays(7).toString())
                    .param("to", today.toString())
                    .header("Authorization", token));
            mvc.perform(get("/api/v1/metric-details/" + metric).header("Authorization", token));
        }
    }

    @Test
    void Q29_viewingChangesNothing() throws Exception {
        Map<String, Long> before = snapshot();

        viewEverything(manager);
        viewEverything(office);
        viewEverything(floorManager);

        assertThat(snapshot()).isEqualTo(before);
    }

    private int exported() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'REPORT_EXPORTED'", Integer.class);
    }

    private int files() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM file", Integer.class);
    }

    private org.springframework.test.web.servlet.ResultActions export(String token) throws Exception {
        LocalDate today = time.today();
        return mvc.perform(post("/api/v1/reports/INCURRED_COSTS/export")
                .header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"from": "%s", "to": "%s", "includeIdentity": false}
                        """.formatted(today.minusDays(7), today)));
    }

    @Test
    void Q29_onlySuccessfulExportWrites_oneAuditRowOneFile() throws Exception {
        Map<String, Long> before = snapshot();
        int exportedBefore = exported();
        int filesBefore = files();

        export(manager).andExpect(status().isCreated());
        export(office).andExpect(status().isCreated());
        export(floorManager).andExpect(status().isForbidden());

        assertThat(exported()).isEqualTo(exportedBefore + 2);
        assertThat(files()).isEqualTo(filesBefore + 2);
        Map<String, Long> after = snapshot();
        after.keySet().removeAll(EXPORT_WRITES);
        before.keySet().removeAll(EXPORT_WRITES);
        assertThat(after).isEqualTo(before);
    }
}
