package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.care.api.events.CarePlanReviewNeeded;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.events.AssessmentChangedCareLevelOrNeeds;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RecordApplicationEvents
class BR_M04_20_CarePlanReviewIT extends MySqlIntegrationTestBase {
    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    ApplicationEvents events;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    ApproverApi approvers;

    @Test
    void BR_M04_20_needsOnlyReassessmentNotifiesWithoutCreatingBusinessRecords() {
        long resident = TestUsers.nextId();
        long assessment = TestUsers.nextId();
        assertReview(
                resident,
                assessment,
                new AssessmentChangedCareLevelOrNeeds(
                        resident, assessment, CareLevel.BASIC, CareLevel.BASIC, Set.of(1L), Set.of(2L)));
    }

    @Test
    void BR_M04_20_careLevelAddendumNotifiesWithoutCreatingBusinessRecords() {
        long resident = TestUsers.nextId();
        long addendum = TestUsers.nextId();
        assertReview(resident, addendum, addendum(resident, addendum, "CARE_LEVEL"));
    }

    @Test
    void BR_M04_20_unrelatedAddendumDoesNotNotify() {
        publisher.publishEvent(addendum(TestUsers.nextId(), TestUsers.nextId(), "RESIDENCE_TYPE"));
        assertThat(events.stream(CarePlanReviewNeeded.class)).isEmpty();
    }

    private void assertReview(long resident, long source, Object event) {
        long approver = TestUsers.nextId();
        when(approvers.approvers(
                        new PermissionKey(Function.CARE_PLAN, Operation.APPROVE),
                        new Target("M04.CARE_PLAN", null, resident, null)))
                .thenReturn(List.of(approver));
        var before = businessCounts();
        publisher.publishEvent(event);
        var notices = events.stream(CarePlanReviewNeeded.class).toList();
        assertThat(notices).allSatisfy(notice -> {
            assertThat(notice.residentId()).isEqualTo(resident);
            assertThat(notice.context().sourceId()).isEqualTo(source);
        });
        var recipients = notices.stream()
                .flatMap(notice -> notice.context().recipients().stream())
                .toList();
        assertThat(recipients)
                .containsExactlyInAnyOrder(
                        new RecipientGroup.AssignedNurse(resident, new Substitution.Default()),
                        new RecipientGroup.Approvers("M04.CARE_PLAN", List.of(approver)));
        assertThat(notices).extracting(CarePlanReviewNeeded::eventKey).doesNotHaveDuplicates();
        assertThat(businessCounts()).isEqualTo(before);
    }

    private List<Long> businessCounts() {
        return List.of(
                jdbc.queryForObject("SELECT COUNT(*) FROM care_plan", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM care_task", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM approval_request", Long.class));
    }

    private static AddendumApplied addendum(long resident, long id, String type) {
        return new AddendumApplied(id, TestUsers.nextId(), resident, Set.of(type), LocalDate.of(2026, 11, 3));
    }
}
