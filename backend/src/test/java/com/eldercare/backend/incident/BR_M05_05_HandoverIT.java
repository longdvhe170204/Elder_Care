package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.AssigneeChanged;
import com.eldercare.backend.incident.internal.alert.service.DefaultAlertHandoverApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bàn giao ca được xác nhận (BR-M05-05, BR-M09-08, FR-037, CHK016; 007 module-interfaces mục 1.5): cảnh báo, sự cố đang
 * mở có người phụ trách thuộc ca trước chuyển sang Điều dưỡng phụ trách ca sau (không có thì Người phụ trách ca sau);
 * giữ trạng thái, cấp, hạn; Bác sĩ, Quản lý viện không bị chuyển.
 */
class BR_M05_05_HandoverIT extends AlertHandoverTestBase {
    private DefaultAlertHandoverApi api;

    @org.springframework.beans.factory.annotation.Autowired
    DefaultAlertHandoverApi bean;

    @BeforeEach
    void api() {
        api = new DefaultAlertHandoverApi(items, transfer, time, null);
    }

    @Test
    void BR_M05_05_confirmedHandoverMovesAlertOfPreviousShiftToNextNurse() {
        long alert = insertAlert("RECEIVED", DAY_NURSE, false);

        confirm();

        var row = alertRow(alert);
        assertThat(row.assignee()).isEqualTo(NIGHT_NURSE);
        assertThat(row.status()).isEqualTo("RECEIVED");
        assertThat(row.escalationLevel()).isEqualTo(1);
        assertThat(row.deadline()).isEqualTo(DEADLINE);
        assertThat(published).singleElement().isInstanceOfSatisfying(AssigneeChanged.class, event -> {
            assertThat(event.sourceId()).isEqualTo(alert);
            assertThat(event.newAssigneeId()).isEqualTo(NIGHT_NURSE);
            assertThat(event.notificationRequests()).hasSize(1);
        });
    }

    private void confirm() {
        transaction.executeWithoutResult(status -> api.onHandoverConfirmed(1, PREVIOUS_SHIFT, NEXT_SHIFT, floor));
    }
}
