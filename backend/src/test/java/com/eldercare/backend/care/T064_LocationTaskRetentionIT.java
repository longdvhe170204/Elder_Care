package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T064/R-15: task vị trí tính hạn theo {@code M04.LOCATION_TASK}, {@code M04.LOCATION_CARE_RESULT} nhưng dòng bảng chung
 * (lịch sử, dấu đã nhắc, sự kiện) ghi bằng {@code M04.TASK}, {@code M04.CARE_RESULT}. Job loại bỏ phải xóa đúng các dòng
 * đó cùng task vị trí, không đụng dòng của task gắn người cùng loại đối tượng (000 module-interfaces mục 1.7).
 */
@Import(SwitchableUserConfig.class)
class T064_LocationTaskRetentionIT extends MySqlIntegrationTestBase {

    private static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    /** Quá CFG-M15-04 (mặc định 10 năm) so với đồng hồ thật hoặc giả lập. */
    private static final String OLD = "2010-01-04 10:00:00";

    @Autowired
    ExpiredDataPurgeJob job;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sharedRowsOfExpiredLocationTask_arePurgedWithIt_personTaskRowsStay() {
        long location = task(null, TestUsers.nextId(), false);
        long result = task(null, TestUsers.nextId(), true);
        long person = task(TestUsers.nextId(), null, false);
        sharedRows("M04.TASK", location);
        sharedRows("M04.TASK", result);
        sharedRows("M04.CARE_RESULT", result);
        sharedRows("M04.TASK", person);

        runJob();

        assertThat(taskExists(location)).isFalse();
        assertThat(taskExists(result)).isFalse();
        assertThat(sharedRowsOf("M04.TASK", location)).isZero();
        assertThat(sharedRowsOf("M04.TASK", result)).isZero();
        assertThat(sharedRowsOf("M04.CARE_RESULT", result)).isZero();
        // Task gắn người chưa tới hạn (hồ sơ chưa ở trạng thái cuối): dòng M04.TASK của nó còn nguyên.
        assertThat(taskExists(person)).isTrue();
        assertThat(sharedRowsOf("M04.TASK", person)).isEqualTo(2);
    }

    private void runJob() {
        user.set(SCHEDULER);
        try {
            job.run();
        } finally {
            user.reset();
        }
    }

    private long task(Long residentId, Long locationId, boolean withResult) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_task (id, resident_id, location_type, location_id, task_type_id, basis_type, shift_id,"
                        + " floor_area_id, planned_at, window_from, window_to, importance, performer_role, status, data,"
                        + " closed_at, result_type, result_performed_at, result_recorded_at, result_recorded_by,"
                        + " result_permission_basis, result_values, created_by, created_at)"
                        + " VALUES (?, ?, ?, ?, 1, 'AD_HOC', 1, 1, ?, ?, ?, 'NORMAL', 'CAREGIVER', ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
                id,
                residentId,
                locationId == null ? null : "ROOM",
                locationId,
                OLD,
                "2010-01-04 09:45:00",
                "2010-01-04 10:15:00",
                withResult ? "COMPLETED" : "CANCELLED",
                // Nội dung đủ như công việc thật: dòng này nằm lại tới khi job loại bỏ chạy, chỉ tiêu 016 đọc toàn bảng
                // (016 T111)
                "{\"publicTaskTypeId\":1,\"taskTypeName\":\"[Kiểm thử lưu giữ]\",\"content\":\"Kiểm thử\",\"resultType\":\"DONE\",\"billable\":false,\"certificates\":[]}",
                OLD,
                withResult ? "DONE" : null,
                withResult ? OLD : null,
                withResult ? OLD : null,
                withResult ? 1L : null,
                withResult ? "ASSIGNED" : null,
                withResult ? "{}" : null,
                "2010-01-04 09:00:00");
        return id;
    }

    private void sharedRows(String type, long id) {
        jdbc.update(
                "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at,"
                        + " by_system) VALUES (?, ?, 'status', 'M04', 'POINT', 'CANCELLED', ?, TRUE)",
                type,
                id,
                OLD);
        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at) VALUES"
                        + " ('M04.TASK_OVERDUE', ?, ?, 'INITIAL', ?)",
                type,
                id,
                OLD);
    }

    private int sharedRowsOf(String type, long id) {
        int n = 0;
        for (String table : new String[] {"status_history", "reminder_marker"}) {
            n += jdbc.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE entity_type = ? AND entity_id = ?",
                    Integer.class,
                    type,
                    id);
        }
        return n;
    }

    private boolean taskExists(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE id = ?", Integer.class, id) == 1;
    }
}
