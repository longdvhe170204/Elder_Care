package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** T046: đọc lại file, nguyên tử, trùng mọi trạng thái, giới hạn và hiệu năng. */
@Import(StaffImportIT.Users.class)
class StaffImportIT extends MySqlIntegrationTestBase {
    static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final List<String> HEADERS =
            List.of("Họ tên", "Chức danh", "Số điện thoại", "Ngày bắt đầu làm việc", "Chuyên môn", "Email");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    ApplicationContext context;

    @Autowired
    JsonMapper json;

    private String phone;
    private long actor;

    @BeforeEach
    void setup() {
        var u = new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER);
        current.set(u.user());
        actor = u.accountId();
        phone = String.format("0%09d", TestUsers.nextId() % 1000000000L);
    }

    private Object service() throws Exception {
        return context.getBean(
                Class.forName("com.eldercare.backend.staffing.staff.internal.service.StaffImportService"));
    }

    private Map<?, ?> call(String method, byte[] file) throws Exception {
        return json.convertValue(
                ReflectionTestUtils.invokeMethod(service(), method, "nhan-vien.xlsx", MIME, file), Map.class);
    }

    static byte[] file(List<? extends List<?>> rows) {
        var excel = new ExcelDocument();
        excel.sheet("NhanVien")
                .table(
                        HEADERS.stream()
                                .map(h -> new ExcelDocument.Column(h, ExcelDocument.Format.TEXT))
                                .toList(),
                        rows);
        return excel.toBytes();
    }

    private List<?> row(String phone) {
        return Arrays.asList("[Nhân viên import]", "Điều dưỡng", phone, "25/10/2026", null, null);
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM staff_profile", Integer.class);
    }

    @Test
    void FR_001b_previewWritesNothingAndNormalizesPhoneAndDate() throws Exception {
        int before = count();
        long logs = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
        var p = call("preview", file(List.of(row(phone.substring(0, 4) + " .- " + phone.substring(4)))));
        assertThat(p.get("canConfirm")).isEqualTo(true);
        var r = (Map<?, ?>) ((List<?>) p.get("rows")).getFirst();
        assertThat(r.get("phoneNumber")).isEqualTo(phone);
        assertThat(r.get("startDate")).isEqualTo("2026-10-25");
        assertThat(r.get("rowNumber")).isEqualTo(2);
        assertThat(count()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class))
                .isEqualTo(logs);
    }

    @Test
    void FR_001b_rowErrorsIdentifyEveryColumnAndDuplicateRow() throws Exception {
        var p = call(
                "preview",
                file(List.of(Arrays.asList(null, null, "abc", "31/02/2026", null, "bad"), row(phone), row(phone))));
        assertThat(p.get("canConfirm")).isEqualTo(false);
        var errors = (List<Map<String, Object>>) p.get("errors");
        assertThat(errors.stream().map(e -> e.get("errorCode")))
                .contains(
                        "REQUIRED_CELL_MISSING",
                        "INVALID_PHONE_NUMBER_FORMAT",
                        "INVALID_DATE_FORMAT",
                        "INVALID_EMAIL_FORMAT",
                        "DUPLICATE_FILE_ROW");
        assertThat(errors).allSatisfy(e -> {
            assertThat(e.get("column")).isNotNull();
            assertThat(e.get("reason")).isNotNull();
        });
        assertThat(errors.stream()
                        .filter(e -> e.get("errorCode").equals("DUPLICATE_FILE_ROW"))
                        .findFirst()
                        .orElseThrow()
                        .get("duplicateRowNumber"))
                .isEqualTo(3);
    }

    @Test
    void FR_001b_confirmRevalidatesAndCreatesZeroProfilesIfPhoneBecameDuplicate() throws Exception {
        var bytes = file(List.of(row(phone)));
        assertThat(call("preview", bytes).get("canConfirm")).isEqualTo(true);
        long existing = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_profile(full_name,position,normalized_phone,status) VALUES ('[Đã nghỉ việc]','Điều dưỡng',?,'RESIGNED')",
                phone);
        int before = count();
        assertThatThrownBy(() -> call("confirm", bytes))
                .satisfies(e -> assertThat(e.getClass().getSimpleName()).isEqualTo("StaffImportRejected"));
        assertThat(count()).isEqualTo(before);
        var errors = (List<Map<String, Object>>) call("preview", bytes).get("errors");
        assertThat(((Number) errors.getFirst().get("duplicateStaffId")).longValue())
                .isEqualTo(existing);
    }

    @Test
    void FR_001c_confirmCreatesOnlyProfilesAndSingleAuditWithoutOriginalFile() throws Exception {
        var r = call("confirm", file(List.of(row(phone))));
        long batch = ((Number) r.get("importBatchId")).longValue();
        assertThat(r.get("createdProfileCount")).isEqualTo(1);
        long id = ((Number) ((List<?>) r.get("staffIds")).getFirst()).longValue();
        assertThat(jdbc.queryForObject("SELECT source FROM staff_profile WHERE id=?", String.class, id))
                .isEqualTo("IMPORT");
        assertThat(jdbc.queryForObject("SELECT account_id FROM staff_profile WHERE id=?", Long.class, id))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_department WHERE staff_id=?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='STAFF_IMPORTED' AND entity_id=? AND actor_id=?",
                        Integer.class,
                        Long.toString(batch),
                        actor))
                .isOne();
    }

    @Test
    void BR_M09_18_invalidTemplateMimeAndMissingFileFailBeforeAnyWrite() throws Exception {
        assertThatThrownBy(() -> call("preview", new byte[] {1, 2, 3}))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("INVALID_IMPORT_TEMPLATE");
        var x = new ExcelDocument();
        x.sheet("NhanVien").table(List.of(new ExcelDocument.Column("Sai cột", ExcelDocument.Format.TEXT)), List.of());
        assertThatThrownBy(() -> call("preview", x.toBytes())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                        service(), "preview", "nhan-vien.xlsx", "text/plain", file(List.of(row(phone)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_001b_nonManagerCannotImport() throws Exception {
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        assertThatThrownBy(() -> call("preview", file(List.of(row(phone)))))
                .isInstanceOf(com.eldercare.backend.common.error.PermissionDeniedException.class);
    }

    @Test
    void SC_013_fiveHundredRowsPreviewAndConfirmEachWithinTenSeconds() throws Exception {
        var rows = new ArrayList<List<?>>();
        long base = TestUsers.nextId() % 1000000L;
        for (int n = 0; n < 500; n++) rows.add(row(String.format("09%08d", base + n)));
        var bytes = file(rows);
        long start = System.nanoTime();
        assertThat(call("preview", bytes).get("canConfirm")).isEqualTo(true);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
        start = System.nanoTime();
        assertThat(call("confirm", bytes).get("createdProfileCount")).isEqualTo(500);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
    }

    @Test
    void FR_001b_rowLimitUsesRuntimeParameterAndRejectsWholeFile() throws Exception {
        var rows = new ArrayList<List<?>>();
        for (int n = 0; n < 501; n++) rows.add(row(phone));
        assertThatThrownBy(() -> call("preview", file(rows)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("IMPORT_ROW_LIMIT_EXCEEDED");
    }

    @Test
    void FR_001b_concurrentConfirmForSamePhoneCreatesOneProfile() throws Exception {
        byte[] data = file(List.of(row(phone)));
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var threads = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var actions = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int n = 0; n < 2; n++)
                actions.add(threads.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        call("confirm", data);
                        return true;
                    } catch (com.eldercare.backend.staffing.staff.internal.domain.StaffImportRejected expected) {
                        return false;
                    }
                }));
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int success = 0;
            for (var action : actions) if (action.get(15, java.util.concurrent.TimeUnit.SECONDS)) success++;
            assertThat(success).isOne();
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM staff_profile WHERE normalized_phone=?", Integer.class, phone))
                    .isOne();
        }
    }

    @Test
    void FR_001b_excelDateCellsAreAcceptedAndOutOfOrderHeadersAreRejected() throws Exception {
        var x = new ExcelDocument();
        x.sheet("NhanVien")
                .table(
                        HEADERS.stream()
                                .map(h -> new ExcelDocument.Column(
                                        h,
                                        h.equals("Ngày bắt đầu làm việc")
                                                ? ExcelDocument.Format.DATE
                                                : ExcelDocument.Format.TEXT))
                                .toList(),
                        List.of(Arrays.asList(
                                "[Ngày Excel]", "Điều dưỡng", phone, LocalDate.of(2026, 10, 25), null, null)));
        assertThat(call("preview", x.toBytes()).get("canConfirm")).isEqualTo(true);
        var wrong = new ExcelDocument();
        var headers = new ArrayList<>(HEADERS);
        Collections.swap(headers, 0, 1);
        wrong.sheet("NhanVien")
                .table(
                        headers.stream()
                                .map(h -> new ExcelDocument.Column(h, ExcelDocument.Format.TEXT))
                                .toList(),
                        List.of());
        assertThatThrownBy(() -> call("preview", wrong.toBytes())).isInstanceOf(BusinessException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
