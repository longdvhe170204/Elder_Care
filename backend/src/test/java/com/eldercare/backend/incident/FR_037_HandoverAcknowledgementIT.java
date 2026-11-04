package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView;
import com.eldercare.backend.incident.internal.alert.service.AlertProvisionalTakeoverService;
import com.eldercare.backend.incident.internal.alert.service.DefaultAlertHandoverApi;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tạm nhận khi ca sau đã bắt đầu mà bàn giao chưa xác nhận (FR-037 ngoại lệ, Q-94, research S-18): mục có người phụ
 * trách thuộc ca trước và không có tên trong ca nào đang diễn ra chuyển tạm sang Người phụ trách ca sau (không có thì
 * Trưởng tầng), mang dấu tạm nhận; giữ trạng thái, cấp, hạn; xác nhận bàn giao chuyển tiếp và gỡ dấu.
 */
class FR_037_HandoverAcknowledgementIT extends AlertHandoverTestBase {
    @Autowired
    ReminderMarkers markers;

    private AlertProvisionalTakeoverService takeover;

    @BeforeEach
    void takeover() {
        when(staff.startedWithoutConfirmedHandover(any(), any()))
                .thenReturn(List.of(new UnconfirmedShiftHandoverView(PREVIOUS_SHIFT, NEXT_SHIFT, floor)));
        takeover = new AlertProvisionalTakeoverService(
                transfer, transaction, markers, staff, java.time.Duration.ofDays(1));
    }

    @Test
    void Q_94_unconfirmedHandoverMovesItemProvisionallyToShiftLeadOnce() {
        long alert = insertAlert("ESCALATED", DAY_NURSE, false);
        long incident = insertIncident("IN_PROGRESS", DAY_NURSE, false);

        takeover.run(NOW);
        takeover.run(NOW.plusSeconds(60)); // chạy lại: không chuyển, không báo lần nữa

        var row = alertRow(alert);
        assertThat(row.assignee()).isEqualTo(SHIFT_LEAD);
        assertThat(row.provisional()).isTrue();
        assertThat(row.status()).isEqualTo("ESCALATED");
        assertThat(row.escalationLevel()).isEqualTo(1);
        assertThat(row.deadline()).isEqualTo(DEADLINE);
        assertThat(incidentAssignee(incident)).isEqualTo(SHIFT_LEAD);
        assertThat(incidentProvisional(incident)).isTrue();
        assertThat(published).hasSize(2);
    }

    @Test
    void Q_94_assigneeStillOnAnOngoingShiftKeepsItem() {
        when(staff.inProgressShiftMember(eq(DAY_NURSE), any())).thenReturn(true);
        long alert = insertAlert("RECEIVED", DAY_NURSE, false);

        takeover.run(NOW);

        assertThat(alertRow(alert).assignee()).isEqualTo(DAY_NURSE);
        assertThat(alertRow(alert).provisional()).isFalse();
    }

    @Test
    void Q_94_noShiftLeadFallsBackToFloorManager() {
        when(scopes.shiftLead(eq(floor), any())).thenReturn(Optional.empty());
        long alert = insertAlert("RECEIVED", DAY_NURSE, false);

        takeover.run(NOW);

        assertThat(alertRow(alert).assignee()).isEqualTo(FLOOR_MANAGER);
    }

    @Test
    void CHK016_doctorAssigneeIsNotTakenOver() {
        long alert = insertAlert("RECEIVED", DOCTOR, false);

        takeover.run(NOW);

        assertThat(alertRow(alert).assignee()).isEqualTo(DOCTOR);
    }

    @Test
    void FR_037_confirmationMovesProvisionalItemAndClearsMark() {
        long alert = insertAlert("RECEIVED", DAY_NURSE, false);
        takeover.run(NOW);

        var api = new DefaultAlertHandoverApi(items, transfer, time, null);
        transaction.executeWithoutResult(status -> api.onHandoverConfirmed(1, PREVIOUS_SHIFT, NEXT_SHIFT, floor));

        var row = alertRow(alert);
        assertThat(row.assignee()).isEqualTo(NIGHT_NURSE);
        assertThat(row.provisional()).isFalse();
        assertThat(row.status()).isEqualTo("RECEIVED");
    }
}
