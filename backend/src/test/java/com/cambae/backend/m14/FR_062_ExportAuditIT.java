package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.TestDoseMetric.Dose;
import com.cambae.backend.m14.TestDoseMetric.DoseSource;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * FR-061, FR-062, US9 (016 T025, tên logic {@code FR_062_NhatKyXuatIT}; quickstart Q25 → Q28): lần xuất ghi một bản nhật ký
 * {@code REPORT_EXPORTED} trong transaction trước khi trả mã tệp (bộ lọc, khoảng, phạm vi, số dòng, dấu định danh ở chi
 * tiết; mục đích là lý do); lỗi ghi nhật ký thì không có tệp; tệp XLSX ở kho tệp loại {@code M14.REPORT_EXPORT}, chỉ người
 * xuất và Quản lý viện tải được; Quản lý viện thấy mọi lần xuất, người khác chỉ lần của mình.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, FakeFileConfig.class, TestDoseMetric.class})
class FR_062_ExportAuditIT extends MySqlIntegrationTestBase {

    private static final String EXPORT = "/api/v1/reports/HEALTH_MEDICATION/export";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    DoseSource doses;

    @MockitoSpyBean
    AuditLogApi auditLog;

    TestUsers users;
    TestUser manager;
    TestUser floorManager;
    long floor1;
    long floor2;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        floorManager = users.staff(Role.FLOOR_MANAGER);
        floor1 = TestUsers.nextId();
        floor2 = TestUsers.nextId();
        assignments.assign(
                floorManager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor1,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(30)),
                        null));
        Instant yesterday = time.now().minus(Duration.ofDays(1));
        doses.doses.addAll(List.of(
                new Dose("d-" + UUID.randomUUID(), yesterday, floor1, 1L, "Nguyễn Văn A", "Sáng"),
                new Dose("d-" + UUID.randomUUID(), yesterday, floor1, 2L, "Trần Thị B", "Sáng"),
                new Dose("d-" + UUID.randomUUID(), yesterday, floor1, 1L, "Nguyễn Văn A", "Tối"),
                new Dose("d-" + UUID.randomUUID(), yesterday, floor2, 3L, "Lê Văn C", "Sáng")));
    }

    @AfterEach
    void reset() {
        assignments.unassign(floorManager.staffId());
        doses.doses.clear();
    }

    private ResultActions export(TestUser u, String body) throws Exception {
        return mvc.perform(post(EXPORT)
                .header("Authorization", TestUsers.bearer(users.session(u.accountId())))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String body(boolean includeIdentity, String purpose) {
        LocalDate today = time.today();
        return """
                {"from": "%s", "to": "%s", "filters": {"floorAreaId": []}, "includeIdentity": %s%s}
                """.formatted(
                        today.minusDays(7),
                        today,
                        includeIdentity,
                        purpose == null ? "" : ", \"purpose\": \"" + purpose + "\"");
    }

    private Map<String, Object> auditRow(long fileId) {
        return jdbc.queryForMap(
                "SELECT id, action, actor_id, reason, data_before, data_after,"
                        + " JSON_UNQUOTE(JSON_EXTRACT(details, '$.group')) AS report_group,"
                        + " JSON_EXTRACT(details, '$.includeIdentity') AS include_identity,"
                        + " JSON_EXTRACT(details, '$.rowCount') AS row_count,"
                        + " JSON_UNQUOTE(JSON_EXTRACT(details, '$.from')) AS from_date"
                        + " FROM audit_log WHERE entity_type = 'M14.REPORT_EXPORT' AND entity_id = ?",
                String.valueOf(fileId));
    }

    private int auditCount(TestUser u) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'REPORT_EXPORTED' AND actor_id = ?",
                Integer.class,
                u.accountId());
    }

    private int fileCount(TestUser u) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM file WHERE uploaded_by = ?", Integer.class, u.accountId());
    }

    private byte[] download(TestUser u, long fileId, int expectedStatus) throws Exception {
        return mvc.perform(get("/api/v1/files/" + fileId)
                        .header("Authorization", TestUsers.bearer(users.session(u.accountId()))))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    /** Mọi chữ trong các trang tính, để kiểm có hay không có định danh. */
    private static String text(byte[] xlsx) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (ReadableWorkbook wb = new ReadableWorkbook(new ByteArrayInputStream(xlsx))) {
            wb.getSheets().forEach(sheet -> {
                try (Stream<Row> rows = sheet.openStream()) {
                    rows.forEach(r -> r.forEach(c ->
                            sb.append(c == null ? "" : ((Cell) c).getText()).append('|')));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        return sb.toString();
    }

    private static long fileId(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.fileId")).longValue();
    }

    @Test
    void Q25_manager_withIdentityAndPurpose_logsBeforeReturningFile() throws Exception {
        ResultActions result = export(manager, body(true, "họp giao ban an toàn thuốc"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").isNumber())
                .andExpect(jsonPath("$.auditLogId").isNumber());
        long fileId = fileId(result);
        long auditLogId = ((Number)
                        JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.auditLogId"))
                .longValue();

        Map<String, Object> row = auditRow(fileId);
        assertThat(((Number) row.get("id")).longValue()).isEqualTo(auditLogId);
        assertThat(row)
                .containsEntry("action", "REPORT_EXPORTED")
                .containsEntry("reason", "họp giao ban an toàn thuốc")
                .containsEntry("data_before", null)
                .containsEntry("data_after", null)
                .containsEntry("report_group", "HEALTH_MEDICATION")
                .containsEntry("from_date", time.today().minusDays(7).toString());
        assertThat(String.valueOf(row.get("include_identity"))).isEqualTo("true");
        // tổng, hai ca, bốn liều chi tiết
        assertThat(String.valueOf(row.get("row_count"))).isEqualTo("7");
        assertThat(jdbc.queryForObject("SELECT owner_type FROM file WHERE id = ?", String.class, fileId))
                .isEqualTo("M14.REPORT_EXPORT");

        String content = text(download(manager, fileId, 200));
        assertThat(content).contains("Nguyễn Văn A", "Lê Văn C", "Tối", "họp giao ban an toàn thuốc");
    }

    @Test
    void Q25_manager_withIdentity_withoutPurpose_422_nothingLogged() throws Exception {
        int before = auditCount(manager);

        export(manager, body(true, null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("PURPOSE_REQUIRED"));

        assertThat(auditCount(manager)).isEqualTo(before);
        assertThat(fileCount(manager)).isZero();
    }

    @Test
    void Q26_floorManager_aggregateOnly_ownFloor_noIdentity_noSinglePersonGroup() throws Exception {
        long fileId = fileId(export(floorManager, body(false, null)).andExpect(status().isCreated()));

        Map<String, Object> row = auditRow(fileId);
        assertThat(String.valueOf(row.get("include_identity"))).isEqualTo("false");
        assertThat(((Number) row.get("actor_id")).longValue()).isEqualTo(floorManager.accountId());
        String content = text(download(floorManager, fileId, 200));
        assertThat(content)
                .contains(TestDoseMetric.CODE, "Sáng")
                // ca Tối chỉ có một người; tầng 2 ngoài phạm vi
                .doesNotContain("Nguyễn Văn A", "Trần Thị B", "Lê Văn C", "Tối");
    }

    @Test
    void US9_1_floorManager_withIdentity_403() throws Exception {
        export(floorManager, body(true, "họp"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PERMISSION_DENIED"));
        assertThat(fileCount(floorManager)).isZero();
    }

    @Test
    void FR_060_rangeTooLong_422() throws Exception {
        LocalDate today = time.today();
        export(manager, """
                        {"from": "%s", "to": "%s"}
                        """.formatted(today.minusYears(2), today))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("RANGE_EXCEEDS_MAXIMUM"));
    }

    @Test
    void Q27_auditLogFails_noFileDelivered() throws Exception {
        // stub trên đối tượng đích: gọi qua proxy transaction thì write (MANDATORY) chạy trước Mockito
        AuditLogApi target = AopTestUtils.getUltimateTargetObject(auditLog);
        doThrow(new IllegalStateException("không ghi được nhật ký"))
                .when(target)
                .write(any());

        // lỗi không có mã đi tới bộ xử lý mặc định (500 ở máy chủ thật); MockMvc ném lại
        assertThatThrownBy(() -> export(floorManager, body(false, null))).hasRootCauseMessage("không ghi được nhật ký");

        assertThat(fileCount(floorManager)).isZero();
        assertThat(auditCount(floorManager)).isZero();
    }

    @Test
    void FR_062_managerSeesEveryExport_othersOnlyTheirOwn_downloadByExporterOrManager() throws Exception {
        long own = fileId(export(floorManager, body(false, null)).andExpect(status().isCreated()));
        long managers = fileId(export(manager, body(false, null)).andExpect(status().isCreated()));
        TestUser otherFloorManager = users.staff(Role.FLOOR_MANAGER);

        mvc.perform(get("/api/v1/reports/exports")
                        .header("Authorization", TestUsers.bearer(users.session(floorManager.accountId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fileId").value(org.hamcrest.Matchers.contains((int) own)))
                .andExpect(jsonPath("$[0].group").value("HEALTH_MEDICATION"))
                .andExpect(jsonPath("$[0].includeIdentity").value(false));
        mvc.perform(get("/api/v1/reports/exports")
                        .header("Authorization", TestUsers.bearer(users.session(manager.accountId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fileId").value(org.hamcrest.Matchers.hasItems((int) own, (int) managers)));

        download(manager, own, 200);
        download(otherFloorManager, own, 403);
        download(floorManager, managers, 403);
    }
}
