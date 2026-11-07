package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.registration.domain.ReminderType;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationReminderStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 018 T004 ({@code ChayBuM02KhaiBaoIT}; research Z-06, NFR-04, Q-306): bốn việc khai báo là việc của job hằng ngày {@code
 * M02_DAILY} (004 T012, BR-M02-19): chạy theo thứ tự, chỉ xử lý ngày mới nhất (việc xét trạng thái tại ngày chạy nên ngày
 * lỡ ghi "bỏ qua", lần chạy đầu sau đó xử lý đủ, SC-002); chạy bù, mỗi việc mỗi ngày một lần, việc lỗi không chặn việc khác
 * do {@code BR_M02_19_CatchUpRunIT} kiểm. Khóa nhắc {@code (loại, người, ngày)} không cho nhắc trùng.
 */
class RegistrationCatchUpIT extends MySqlIntegrationTestBase {

    static final long RESIDENT = 918_500_001L;

    @Autowired
    ObjectProvider<M02DailyTask> tasks;

    @Autowired
    RegistrationReminderStore reminders;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @BeforeEach
    void clean() throws SQLException {
        // Tài khoản ứng dụng không có DELETE trên reminder_marker
        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            statement.executeUpdate(
                    "DELETE FROM reminder_marker WHERE kind = 'M02.REMINDER' AND entity_id = " + RESIDENT);
        }
    }

    @Test
    void Z_06_registrationTasks_areDailyTasksOfM02_inTheOrderOfTheStories() {
        List<M02DailyTask> registration = tasks.orderedStream()
                .filter(task -> task.name().startsWith("REG_"))
                .toList();

        assertThat(registration)
                .extracting(M02DailyTask::name)
                .containsExactly("REG_EXPIRY", "REG_RENEWAL", "REG_OVERDUE", "REG_DEREGISTER");
        assertThat(registration)
                .as("việc xét trạng thái tại ngày chạy: ngày lỡ không chạy lại (SC-002)")
                .allMatch(M02DailyTask::latestDayOnly);
    }

    @Test
    void Z_06_reminderKey_claimsOncePerTypeResidentAndDay() {
        LocalDate day = LocalDate.of(2030, 7, 1);

        List<Boolean> claims = transaction.execute(status -> List.of(
                reminders.claim(ReminderType.OVERDUE, RESIDENT, day),
                reminders.claim(ReminderType.OVERDUE, RESIDENT, day),
                reminders.claim(ReminderType.RENEWAL, RESIDENT, day),
                reminders.claim(ReminderType.OVERDUE, RESIDENT, day.plusDays(1))));

        assertThat(claims).containsExactly(true, false, true, true);
        assertThat(reminders.lastOn(ReminderType.OVERDUE, RESIDENT)).contains(day.plusDays(1));
        assertThat(RegistrationSql.reminderCount(jdbc, "OVERDUE", RESIDENT)).isEqualTo(2);
        assertThat(reminders.lastOn(ReminderType.CREATE, RESIDENT)).isEmpty();
    }
}
