package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.api.CarePlanReviewApi;
import com.eldercare.backend.care.api.events.CarePlanReviewNeeded;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.events.AssessmentChangedCareLevelOrNeeds;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-M04-20, FR-011 (T052): 007 gọi lệnh {@link CarePlanReviewApi} trong transaction của lệnh ghi sự cố ngã, ghi lựa chọn
 * chăm sóc giảm nhẹ (chủ dự án duyệt 08/11, xác nhận 09/11; 005 không nghe sự kiện của 007). Lệnh báo điều dưỡng phụ trách
 * ({@code AssignedNurse}, 009 phân giải) và người có quyền duyệt kế hoạch ({@code ApproverApi} thật); cùng khóa nguồn chỉ
 * báo một lần. Nguồn 001, 004 kiểm ở lượt 03/11 (lớp cũ bỏ khi giảm bộ test 07/11). Q-448 (2026-11-26): mỗi lần báo lưu
 * thêm một yêu cầu xem xét {@code care_plan_review} Đang mở (FR-058), cùng khóa nguồn chỉ một dòng.
 */
@Transactional
@RecordApplicationEvents
class BR_17_CarePlanReviewIT extends MySqlIntegrationTestBase {
    private static final PermissionKey APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);

    @Autowired
    CarePlanReviewApi review;

    @Autowired
    ApproverApi approverApi;

    @Autowired
    ApplicationEvents events;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ApplicationEventPublisher publisher;

    long resident, doctorAccount;

    @BeforeEach
    void setup() {
        resident = TestUsers.nextId();
        doctorAccount = new TestUsers(jdbc, time).staff(Role.DOCTOR).accountId();
    }

    @Test
    void BR_17_palliativeChoices_differentKeysGiveSeparateNotices_repeatedKeyIsIgnored() {
        Instant at = time.now();
        String first = "M05.ALERT:" + TestUsers.nextId() + ":PALLIATIVE_CARE_CHOICE";
        String second = "M05.ALERT:" + TestUsers.nextId() + ":PALLIATIVE_CARE_CHOICE";

        review.palliativeCareChoiceRecorded(resident, first, at);
        var firstNotices = notices();
        assertRecipients(firstNotices);
        assertThat(firstNotices).allSatisfy(n -> assertThat(n.reference()).isEqualTo(first));

        review.palliativeCareChoiceRecorded(resident, second, at);
        review.palliativeCareChoiceRecorded(resident, first, at);

        var all = notices();
        assertThat(all).hasSize(firstNotices.size() * 2);
        Set<String> firstKeys =
                firstNotices.stream().map(CarePlanReviewNeeded::eventKey).collect(Collectors.toSet());
        var secondKeys = all.stream()
                .filter(n -> n.reference().equals(second))
                .map(CarePlanReviewNeeded::eventKey)
                .toList();
        assertThat(secondKeys).hasSameSizeAs(firstKeys).doesNotHaveDuplicates().doesNotContainAnyElementsOf(firstKeys);
    }

    @Test
    void Q448_repeatedFallCreatesOneOpenReview() {
        long incident = TestUsers.nextId();

        review.fallIncidentRecorded(resident, incident);
        review.fallIncidentRecorded(resident, incident);

        assertThat(reviews()).singleElement().satisfies(r -> {
            assertThat(r).containsEntry("source", "M05.INCIDENT");
            assertThat(r).containsEntry("source_id", incident);
            assertThat(r).containsEntry("reference", Long.toString(incident));
            assertThat(r).containsEntry("status", "OPEN");
            assertThat(r.get("raised_at")).isNotNull();
            assertThat(r.get("closed_at")).isNull();
        });
    }

    @Test
    void Q448_palliativeChoiceOpensOneReviewPerKey() {
        String first = "M05.ALERT:" + TestUsers.nextId() + ":PALLIATIVE_CARE_CHOICE";
        String second = "M05.ALERT:" + TestUsers.nextId() + ":PALLIATIVE_CARE_CHOICE";

        review.palliativeCareChoiceRecorded(resident, first, time.now());
        review.palliativeCareChoiceRecorded(resident, first, time.now());
        review.palliativeCareChoiceRecorded(resident, second, time.now());

        assertThat(reviews()).hasSize(2).allSatisfy(r -> {
            assertThat(r).containsEntry("source", "M05.PALLIATIVE_CARE_CHOICE");
            assertThat(r).containsEntry("status", "OPEN");
        });
    }

    @Test
    void Q448_reassessmentOpensReview() {
        long assessment = TestUsers.nextId();

        publisher.publishEvent(new AssessmentChangedCareLevelOrNeeds(
                resident, assessment, CareLevel.BASIC, CareLevel.REGULAR, Set.of(), Set.of()));

        assertThat(reviews()).singleElement().satisfies(r -> {
            assertThat(r).containsEntry("source", "M01.ASSESSMENT");
            assertThat(r).containsEntry("source_id", assessment);
            assertThat(r).containsEntry("status", "OPEN");
        });
        assertThat(notices()).isNotEmpty();
    }

    private List<Map<String, Object>> reviews() {
        return jdbc.queryForList(
                "SELECT source, source_id, reference, status, raised_at, closed_at FROM care_plan_review"
                        + " WHERE resident_id = ? ORDER BY id",
                resident);
    }

    private void assertRecipients(List<CarePlanReviewNeeded> notices) {
        var expected = new ArrayList<RecipientGroup>();
        expected.add(new RecipientGroup.AssignedNurse(resident, new Substitution.Default()));
        for (long account : approverApi.approvers(APPROVE, new Target("M04.CARE_PLAN", null, resident, null)).stream()
                .distinct()
                .toList()) expected.add(new RecipientGroup.Approvers("M04.CARE_PLAN", List.of(account)));
        assertThat(expected).contains(new RecipientGroup.Approvers("M04.CARE_PLAN", List.of(doctorAccount)));
        assertThat(notices.stream().flatMap(n -> n.context().recipients().stream()))
                .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(notices).extracting(CarePlanReviewNeeded::eventKey).doesNotHaveDuplicates();
    }

    private List<CarePlanReviewNeeded> notices() {
        return events.stream(CarePlanReviewNeeded.class)
                .filter(n -> n.residentId() == resident)
                .toList();
    }
}
