package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.incident.api.AlertHandoverApi;
import com.eldercare.backend.incident.api.events.AssigneeChanged;
import com.eldercare.backend.incident.internal.alert.service.DefaultAlertHandoverApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.IllegalTransactionStateException;

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

    @Test
    void BR_M05_05_openIncidentIsMovedToo() {
        long incident = insertIncident("IN_PROGRESS", DAY_NURSE, false);

        confirm();

        assertThat(incidentAssignee(incident)).isEqualTo(NIGHT_NURSE);
    }

    @Test
    void CHK016_doctorAndManagerAssigneesAreNotMoved() {
        long doctorAlert = insertAlert("RECEIVED", DOCTOR, false);
        long managerAlert = insertAlert("IN_PROGRESS", MANAGER, false);

        confirm();

        assertThat(alertRow(doctorAlert).assignee()).isEqualTo(DOCTOR);
        assertThat(alertRow(managerAlert).assignee()).isEqualTo(MANAGER);
        assertThat(published).isEmpty();
    }

    @Test
    void FR_037_assigneeNotInPreviousShiftKeepsAlert() {
        long alert = insertAlert("RECEIVED", OTHER_NURSE, false);

        confirm();

        assertThat(alertRow(alert).assignee()).isEqualTo(OTHER_NURSE);
    }

    @Test
    void FR_037_noNurseInNextShiftFallsBackToShiftLead() {
        noNightNurse();
        long alert = insertAlert("NEW", DAY_NURSE, false);

        confirm();

        assertThat(alertRow(alert).assignee()).isEqualTo(SHIFT_LEAD);
        assertThat(alertRow(alert).status()).isEqualTo("NEW");
    }

    @Test
    void FR_037_closedAlertIsNotTouched() {
        long alert = insertAlert("CLOSED", DAY_NURSE, false);

        confirm();

        assertThat(alertRow(alert).assignee()).isEqualTo(DAY_NURSE);
    }

    @Test
    void FR_037_openListsAlertsAndIncidentsOfFloorAndCurrentStatusReadsBack() {
        long alert = insertAlert("ESCALATED", DAY_NURSE, false);
        long incident = insertIncident("NEW", DAY_NURSE, false);

        var open = api.open(floor, PREVIOUS_SHIFT);

        assertThat(open)
                .extracting(view -> view.group() + ":" + view.id())
                .containsExactlyInAnyOrder("ALERT:" + alert, "INCIDENT:" + incident);
        assertThat(open).allSatisfy(view -> assertThat(view.residentId()).isEqualTo(resident));
        var current = api.currentStatus(List.of(
                new AlertHandoverApi.SourceRef("ALERT", alert), new AlertHandoverApi.SourceRef("INCIDENT", incident)));
        assertThat(current.get(new AlertHandoverApi.SourceRef("ALERT", alert)).status())
                .isEqualTo("ESCALATED");
        assertThat(current.get(new AlertHandoverApi.SourceRef("INCIDENT", incident))
                        .level())
                .isEqualTo("MEDIUM");
    }

    @Test
    void FR_037_confirmationRequiresCallerTransaction() {
        assertThatThrownBy(() -> bean.onHandoverConfirmed(1, PREVIOUS_SHIFT, NEXT_SHIFT, floor))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private void confirm() {
        transaction.executeWithoutResult(status -> api.onHandoverConfirmed(1, PREVIOUS_SHIFT, NEXT_SHIFT, floor));
    }
}
