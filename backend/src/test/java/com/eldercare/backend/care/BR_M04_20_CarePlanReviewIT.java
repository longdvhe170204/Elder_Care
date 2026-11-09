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
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-M04-20, FR-011 (T052): 007 gọi lệnh {@link CarePlanReviewApi} trong transaction của lệnh ghi sự cố ngã, ghi lựa chọn
 * chăm sóc giảm nhẹ (chủ dự án duyệt 08/11, xác nhận 09/11; 005 không nghe sự kiện của 007). Lệnh báo điều dưỡng phụ trách
 * ({@code AssignedNurse}, 009 phân giải) và người có quyền duyệt kế hoạch ({@code ApproverApi} thật); không thêm bản ghi
 * nghiệp vụ (Q35); cùng khóa nguồn chỉ báo một lần. Nguồn 001, 004 kiểm ở lượt 03/11 (lớp cũ bỏ khi giảm bộ test 07/11).
 */
@Transactional
@RecordApplicationEvents
class BR_M04_20_CarePlanReviewIT extends MySqlIntegrationTestBase {
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

    long resident, doctorAccount;

    @BeforeEach
    void setup() {
        resident = TestUsers.nextId();
        doctorAccount = new TestUsers(jdbc, time).staff(Role.DOCTOR).accountId();
    }

    @Test
    void BR_M04_20_fallIncident_notifiesNurseAndApprovers_withoutBusinessRecords_andOnlyOnce() {
        long incident = TestUsers.nextId();
        var before = businessCounts();

        review.fallIncidentRecorded(resident, incident);

        var notices = notices();
        assertThat(notices).allSatisfy(n -> {
            assertThat(n.residentId()).isEqualTo(resident);
            assertThat(n.context().sourceType()).isEqualTo("M05.INCIDENT");
            assertThat(n.context().sourceId()).isEqualTo(incident);
        });
        assertRecipients(notices);
        assertThat(businessCounts())
                .as("Q35: không lập kế hoạch, công việc, yêu cầu")
                .isEqualTo(before);

        review.fallIncidentRecorded(resident, incident);
        assertThat(notices()).as("gọi lặp cùng sự cố không báo lần hai").hasSameSizeAs(notices);
    }

    @Test
    void BR_M04_20_palliativeChoices_differentKeysGiveSeparateNotices_repeatedKeyIsIgnored() {
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

    private List<Long> businessCounts() {
        return List.of(
                jdbc.queryForObject("SELECT COUNT(*) FROM care_plan", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM care_task", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM approval_request", Long.class));
    }
}
