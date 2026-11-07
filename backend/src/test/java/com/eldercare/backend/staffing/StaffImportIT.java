package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** T046: đọc lại file, nguyên tử, trùng mọi trạng thái, giới hạn và hiệu năng. */
@Import(SwitchableUserConfig.class)
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
                Class.forName("com.eldercare.backend.staffing.internal.staff.service.StaffImportService"));
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
}
