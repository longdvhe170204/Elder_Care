package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.AbsenceTaskApi;
import com.eldercare.backend.care.api.dto.CareTimeRange;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.events.AlertClosureRequested;
import com.eldercare.backend.care.api.events.TaskPlanningNeeded;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.OrderUtils;

/** T055, T057: bean chuyển trạng thái của 005 và tính lại khi đính chính chuyến đi (FR-010, FR-020, FR-020a, BR-M04-04). */
class BR_M04_04_ResidentStatusTasksIT extends CareOverdueTestBase {
    StatusTransitionParticipant participant() {
        return context.getBean("careStatusParticipant", StatusTransitionParticipant.class);
    }

    CareTaskStore store() {
        return context.getBean(CareTaskStore.class);
    }

    void apply(StatusCommand command, ResidentStatus from, ResidentStatus to, Instant at) {
        participant()
                .applyEffects(new TransitionResult(
                        residentId, from, to, command, TestUsers.nextId(), at, json.createObjectNode()));
    }

    String rule(long id) {
        return jdbc.queryForObject("SELECT closure_rule FROM care_task WHERE id=?", String.class, id);
    }

    @Test
    void H_02_participantHasNoBlockingConditionAndRunsAfterAllocation() {
        assertThat(participant().interestedCommands())
                .contains(
                        StatusCommand.GRANT_TEMPORARY_ABSENCE,
                        StatusCommand.RECORD_RETURN,
                        StatusCommand.END_RESIDENCY,
                        StatusCommand.CANCEL_DEPARTURE_RECORD);
        assertThat(participant().checkConditions(null)).isEmpty();
        assertThat(OrderUtils.getOrder(org.springframework.aop.support.AopUtils.getTargetClass(participant())))
                .isGreaterThan(20);
    }

    @Test
    void BR_M04_04_temporaryAbsenceCancelsNotYetDueTasksFromDepartureOnly() throws Exception {
        long notYetDue = create();
        long due = createAnother();
        jdbc.update("UPDATE care_task SET status='DUE' WHERE id=?", due);

        apply(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.TEMPORARILY_ABSENT,
                time.now());

        assertThat(store().get(notYetDue, false).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(rule(notYetDue)).isEqualTo("ABSENCE_CANCELLED");
        assertThat(store().get(due, false).status()).isEqualTo(TaskStatus.DUE);
    }

    @Test
    void FR_020_departureAfterPlannedTimeKeepsTheTask() throws Exception {
        long id = create();
        apply(
                StatusCommand.TRANSFER_OUT,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.HOSPITALIZED,
                time.now().plusSeconds(86400));
        assertThat(store().get(id, false).status()).isEqualTo(TaskStatus.NOT_YET_DUE);
    }

    @Test
    void FR_020_returnOnlyNotifiesShiftLeadWithoutCreatingTasks() throws Exception {
        long id = create();
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE resident_id=?", Long.class, residentId);

        apply(StatusCommand.RECORD_RETURN, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.IN_RESIDENCE, time.now());

        assertThat(recorded.stream(TaskPlanningNeeded.class)).singleElement().satisfies(e -> {
            assertThat(e.residentId()).isEqualTo(residentId);
            assertThat(e.reason()).isEqualTo("RETURN");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE resident_id=?", Long.class, residentId))
                .isEqualTo(before);
        assertThat(store().get(id, false).status()).isEqualTo(TaskStatus.NOT_YET_DUE);
    }

    @Test
    void FR_017_completeAdmissionNotifiesTaskPlanning() {
        apply(
                StatusCommand.COMPLETE_ADMISSION,
                ResidentStatus.ADMISSION_IN_PROGRESS,
                ResidentStatus.IN_RESIDENCE,
                time.now());
        assertThat(recorded.stream(TaskPlanningNeeded.class))
                .extracting(TaskPlanningNeeded::reason)
                .containsExactly("ADMISSION_COMPLETED");
    }

    @Test
    void FR_010_finalStatusClosesOpenTasksRequestsAlertClosureAndEndsPlans() throws Exception {
        long notYetDue = create();
        long overdue = createAnother();
        jdbc.update("UPDATE care_task SET status='OVERDUE' WHERE id=?", overdue);
        long effective = TestUsers.nextId();
        long draft = TestUsers.nextId();
        var today = time.today();
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,effective_from,created_by,created_at) VALUES(?,?,1,'EFFECTIVE',?,1,NOW(6))",
                effective,
                residentId,
                today.minusDays(3));
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,effective_from,previous_plan_id,created_by,created_at) VALUES(?,?,2,'DRAFT',?,?,1,NOW(6))",
                draft,
                residentId,
                today.plusDays(1),
                effective);

        apply(StatusCommand.END_RESIDENCY, ResidentStatus.IN_RESIDENCE, ResidentStatus.RESIDENCY_ENDED, time.now());

        assertThat(store().get(notYetDue, false).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(store().get(overdue, false).status()).isEqualTo(TaskStatus.NOT_PERFORMED);
        assertThat(rule(overdue)).isEqualTo("FR_010_FINAL_STATE");
        assertThat(recorded.stream(AlertClosureRequested.class))
                .extracting(AlertClosureRequested::taskId, AlertClosureRequested::reason)
                .containsExactly(tuple(overdue, "FINAL_STATE"));
        assertThat(jdbc.queryForList(
                        "SELECT CONCAT(status,':',end_reason) FROM care_plan WHERE resident_id=? ORDER BY revision",
                        String.class,
                        residentId))
                .containsExactly("EXPIRED:FINAL_STATUS", "CANCELLED:FINAL_STATUS");
    }

    @Test
    void FR_020a_recalculationCancelsTasksInsideTheCorrectedRangeAndNotifiesForTasksNowOutside() throws Exception {
        long inside = create();
        long outside = createAnother();
        var outsideAt = store().get(outside, false).plannedAt();
        jdbc.update(
                "UPDATE care_task SET status='CANCELLED',closure_rule='ABSENCE_CANCELLED',closure_reason='vắng mặt' WHERE id=?",
                outside);
        var insideAt = store().get(inside, false).plannedAt();
        var api = context.getBean(AbsenceTaskApi.class);

        api.recalculate(
                residentId,
                Optional.of(new CareTimeRange(insideAt.minusSeconds(60), outsideAt.plusSeconds(60))),
                Optional.of(new CareTimeRange(insideAt.minusSeconds(60), insideAt.plusSeconds(60))));

        assertThat(store().get(inside, false).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(rule(inside)).isEqualTo("ABSENCE_CANCELLED");
        assertThat(store().get(outside, false).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(recorded.stream(TaskPlanningNeeded.class))
                .extracting(TaskPlanningNeeded::reason)
                .containsExactly("ABSENCE_RECALCULATED");
    }

    @Test
    void FR_020a_cancelledDepartureRecordNotifiesForFutureTasksCancelledByAbsence() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='CANCELLED',closure_rule='ABSENCE_CANCELLED' WHERE id=?", id);

        apply(
                StatusCommand.CANCEL_DEPARTURE_RECORD,
                ResidentStatus.OFF_SITE_ACTIVITY,
                ResidentStatus.IN_RESIDENCE,
                time.now());

        assertThat(recorded.stream(TaskPlanningNeeded.class))
                .extracting(TaskPlanningNeeded::reason)
                .containsExactly("ABSENCE_RECALCULATED");
    }
}
