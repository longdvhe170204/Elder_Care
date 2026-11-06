package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T005 (US7 kịch bản 6; BR-M11-20, CFG-M11-08 mặc định 30 ngày, CFG-M11-09 mặc định 7 ngày; FR-021; SC-007): Đã duyệt
 * quá CFG-M11-08 chưa ghi đã mua thì nhắc Phụ trách bộ phận; Chờ đối chiếu quá CFG-M11-09 thì nhắc Kế toán; mỗi lần vào
 * trạng thái nhắc một lần, chạy lại không nhắc trùng; job không tạo yêu cầu (Q-306).
 */
class BR_M11_20_ReminderIT extends ProcurementTestBase {

    @Autowired
    List<ScheduledJob> jobs;

    ScheduledJob job() {
        return jobs.stream()
                .filter(j -> j.name().equals("REMIND_PROCUREMENT_REQUESTS"))
                .findFirst()
                .orElseThrow();
    }

    void shift(long id, String column, int days) {
        jdbc.update(
                "UPDATE procurement_request SET " + column + " = ? WHERE id = ?",
                LocalDateTime.ofInstant(time.now(), com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .minusDays(days),
                id);
    }

    List<RecipientGroup> reminders(long id, String event) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n ->
                        n.eventType().equals(event) && n.sourceRecord().id().equals(Long.toString(id)))
                .flatMap(n -> n.recipientGroups().stream())
                .toList();
    }

    @Test
    void BR_M11_20_approvedPastThirtyDaysRemindsDepartmentLeadOnce() throws Exception {
        long due = approved(1_800_000);
        long early = approved(1_800_000);
        shift(due, "approved_at", 30);
        shift(early, "approved_at", 29);
        int requestsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM procurement_request", Integer.class);

        job().runNow();
        job().runNow();

        assertThat(reminders(due, "PROCUREMENT_PURCHASE_OVERDUE"))
                .containsExactly(new RecipientGroup.DepartmentLead(kitchen, new Substitution.Default()));
        assertThat(reminders(early, "PROCUREMENT_PURCHASE_OVERDUE")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM procurement_request", Integer.class))
                .as("Q-306: job không tạo yêu cầu")
                .isEqualTo(requestsBefore);
    }

    @Test
    void BR_M11_20_pendingReconciliationPastSevenDaysRemindsAccountants() throws Exception {
        long due = purchased(1_800_000, 1_900_000);
        long early = purchased(1_800_000, 1_900_000);
        shift(due, "purchase_recorded_at", 7);
        shift(early, "purchase_recorded_at", 6);

        job().runNow();
        job().runNow();

        assertThat(reminders(due, "PROCUREMENT_RECONCILIATION_OVERDUE"))
                .containsExactly(new RecipientGroup.FacilityRole(Role.ACCOUNTANT));
        assertThat(reminders(early, "PROCUREMENT_RECONCILIATION_OVERDUE")).isEmpty();
    }

    @Test
    void BR_M11_20_returnedRequestIsRemindedAgainForTheNewPurchaseCycle() throws Exception {
        long id = purchased(1_800_000, 1_900_000);
        shift(id, "purchase_recorded_at", 8);
        job().runNow();
        as(accountant);
        command(id, "return-for-reconciliation", "{\"reason\":\"Thiếu hóa đơn\"}");
        as(kitchenLead);
        command(id, "record-purchased", purchase(1_900_000, kitchenLead));
        shift(id, "purchase_recorded_at", 9);
        job().runNow();

        assertThat(reminders(id, "PROCUREMENT_RECONCILIATION_OVERDUE")).hasSize(2);
    }
}
