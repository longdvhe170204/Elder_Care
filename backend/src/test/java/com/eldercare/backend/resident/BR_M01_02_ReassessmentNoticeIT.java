package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.ReassessmentApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ReassessmentState;
import com.eldercare.backend.resident.api.dto.ReassessmentStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ReassessmentDue;
import com.eldercare.backend.resident.api.events.ReassessmentOverdue;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.ReassessmentNoticeService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

/**
 * 001 T060 (US6 #1 → #4, #10; FR-039 → FR-043, BR-M01-02; đồng hồ giả lập): báo đánh giá lại định kỳ khi lần Đã xác nhận
 * gần nhất qua CFG-M01-02 (mặc định 90 ngày), báo sau sự cố một lần cho mỗi sự cố, hạn CFG-M01-03 (mặc định 48 giờ) tính
 * từ lần báo sớm nhất chưa đáp ứng, cảnh báo quá hạn một lần mỗi lần quá hạn, hồ sơ Điều trị tại bệnh viện chưa tính hạn
 * tới khi Ghi nhận trở về (cùng căn cứ {@code RETURN_FROM_HOSPITAL}, T048), hồ sơ ở trạng thái cuối không báo. Báo là
 * bảng kỹ thuật {@code reassessment_notice}, không tạo đánh giá hay bản ghi yêu cầu nào.
 */
class BR_M01_02_ReassessmentNoticeIT extends StatusTestBase {

    private static final Duration PERIOD = Duration.ofDays(90);
    private static final Duration DEADLINE = Duration.ofHours(48);

    @Autowired
    TestClockControl clock;

    @Autowired
    ReassessmentNoticeService notices;

    @Autowired
    ReassessmentApi reassessments;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    /** Hồ sơ Đang lưu trú có một lần đánh giá Đã xác nhận ở thời điểm đồng hồ hiện tại. */
    private long residentAssessedNow() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        confirmed(id, AssessmentType.INITIAL, CareLevel.REGULAR, "Đầu vào", null);
        moveTo(id, ResidentStatus.IN_RESIDENCE);
        return id;
    }

    private long dueCount(long residentId) {
        return events.stream(ReassessmentDue.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    private List<String> bases(long residentId) {
        return jdbc.queryForList(
                "SELECT basis FROM reassessment_notice WHERE resident_id = ? ORDER BY id", String.class, residentId);
    }

    /** Đồng hồ giả lập vẫn chạy theo giờ thật nên so mốc thời gian với sai số vài giây. */
    private static void assertNear(java.util.Optional<Instant> actual, Instant expected) {
        assertThat(actual).isPresent();
        assertThat(actual.get()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }

    private ReassessmentStatus status(long residentId) {
        return reassessments.status(List.of(residentId)).get(residentId);
    }

    private int noticeRows(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM reassessment_notice WHERE resident_id = ?", Integer.class, residentId);
    }

    @Test
    void US6_1_periodicNoticeOnlyAfterTheValidityPeriod_andOnlyOncePerAssessment() {
        long id = residentAssessedNow();

        clock.advanceBy(PERIOD.minusMinutes(1));
        notices.runPeriodic();
        assertThat(dueCount(id)).as("chưa qua CFG-M01-02").isZero();
        assertThat(status(id).state()).isEqualTo(ReassessmentState.NONE);

        clock.advanceBy(Duration.ofMinutes(2));
        Instant notifiedAt = clock.instant();
        notices.runPeriodic();
        notices.runPeriodic();

        assertThat(dueCount(id)).as("chạy lại không báo trùng (NFR-04)").isEqualTo(1);
        assertThat(bases(id)).containsExactly("PERIODIC");
        ReassessmentStatus status = status(id);
        assertThat(status.state()).isEqualTo(ReassessmentState.DUE);
        assertNear(status.dueAt(), notifiedAt.plus(DEADLINE));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM assessment WHERE resident_id = ? AND type = 'REASSESSMENT'",
                        Integer.class,
                        id))
                .as("không tạo bản ghi yêu cầu hay đánh giá nào")
                .isZero();
    }

    @Test
    void US6_1_periodicSkipsProfilesWithoutAConfirmedAssessmentAndNonOpenStatuses() {
        long noAssessment = newResident();
        setStatus(noAssessment, ResidentStatus.IN_RESIDENCE.name());
        long admitting = newResident();
        setStatus(admitting, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        confirmed(admitting, AssessmentType.INITIAL, CareLevel.BASIC, "Đầu vào", null);

        clock.advanceBy(PERIOD.plusDays(1));
        notices.runPeriodic();

        assertThat(noticeRows(noAssessment)).isZero();
        assertThat(noticeRows(admitting))
                .as("Đang tiếp nhận không thuộc bốn trạng thái của FR-039")
                .isZero();
    }

    @Test
    void US6_2_noticeAfterIncident_oncePerIncident() {
        long id = residentAssessedNow();

        assertThat(notices.notifyAfterIncident(id, 501L)).isTrue();
        assertThat(notices.notifyAfterIncident(id, 501L))
                .as("cùng sự cố, phát lại")
                .isFalse();
        assertThat(notices.notifyAfterIncident(id, 502L)).as("sự cố khác").isTrue();

        assertThat(bases(id)).containsExactly("AFTER_INCIDENT", "AFTER_INCIDENT");
        assertThat(dueCount(id)).isEqualTo(2);
    }

    @Test
    void US6_3_extraBasisIsNotifiedButTheDeadlineStaysAtTheEarliestUnmetNotice() {
        long id = residentAssessedNow();
        clock.advanceBy(PERIOD.plusMinutes(1));
        Instant first = clock.instant();
        notices.runPeriodic();

        clock.advanceBy(Duration.ofHours(10));
        notices.notifyAfterIncident(id, 601L);

        assertThat(dueCount(id)).as("căn cứ mới được báo thêm (FR-042)").isEqualTo(2);
        assertNear(status(id).dueAt(), first.plus(DEADLINE));
    }

    @Test
    void US6_4_overdueAlertOncePerOverdueCycle_andStateOverdueUntilAConfirmedReassessment() {
        long id = residentAssessedNow();
        clock.advanceBy(PERIOD.plusMinutes(1));
        Instant notifiedAt = clock.instant();
        notices.runPeriodic();

        clock.advanceBy(DEADLINE.minusMinutes(1));
        notices.runOverdue();
        assertThat(events.stream(ReassessmentOverdue.class).filter(e -> e.residentId() == id))
                .as("chưa quá CFG-M01-03")
                .isEmpty();

        clock.advanceBy(Duration.ofMinutes(2));
        notices.runOverdue();
        notices.runOverdue();

        assertThat(events.stream(ReassessmentOverdue.class).filter(e -> e.residentId() == id))
                .singleElement()
                .satisfies(e -> assertNear(java.util.Optional.of(e.dueAt()), notifiedAt.plus(DEADLINE)));
        ReassessmentStatus status = status(id);
        assertThat(status.state()).isEqualTo(ReassessmentState.OVERDUE);
        assertNear(status.overdueSince(), notifiedAt.plus(DEADLINE));

        confirmed(id, AssessmentType.REASSESSMENT, CareLevel.REGULAR, "Định kỳ", null);
        assertThat(status(id).state())
                .as("hết cần đánh giá lại khi có lần Đã xác nhận (FR-043)")
                .isEqualTo(ReassessmentState.NONE);

        clock.advanceBy(PERIOD.plusMinutes(1));
        notices.runPeriodic();
        assertThat(status(id).state()).as("chu kỳ mới").isEqualTo(ReassessmentState.DUE);
        clock.advanceBy(DEADLINE.plusMinutes(1));
        notices.runOverdue();
        assertThat(events.stream(ReassessmentOverdue.class).filter(e -> e.residentId() == id))
                .as("mỗi lần quá hạn một cảnh báo")
                .hasSize(2);
    }

    @Test
    void edgeCase_whileHospitalized_periodicNoticeHasNoDeadlineAndNoOverdueAlert() {
        long id = residentAssessedNow();
        moveTo(id, ResidentStatus.HOSPITALIZED);
        clock.advanceBy(PERIOD.plusDays(1));

        notices.runPeriodic();
        clock.advanceBy(Duration.ofDays(10));
        notices.runOverdue();

        assertThat(dueCount(id)).as("bác sĩ vẫn được báo định kỳ").isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT deadline_from FROM reassessment_notice WHERE resident_id = ?",
                        java.sql.Timestamp.class,
                        id))
                .isNull();
        ReassessmentStatus status = status(id);
        assertThat(status.state()).isEqualTo(ReassessmentState.DUE);
        assertThat(status.dueAt()).isEmpty();
        assertThat(events.stream(ReassessmentOverdue.class).filter(e -> e.residentId() == id))
                .isEmpty();
    }

    @Test
    void T048_recordReturnFromHospital_startsTheDeadlineForUnmetNotices_andAddsReturnBasis() {
        long id = residentAssessedNow();
        moveTo(id, ResidentStatus.HOSPITALIZED);
        clock.advanceBy(PERIOD.plusDays(1));
        notices.runPeriodic();
        clock.advanceBy(Duration.ofDays(3));
        Instant returnedAt = clock.instant();

        user.set(office.user());
        long historyId =
                statusService.recordReturn(target(id), reason(), noDetails()).historyId();

        assertThat(bases(id)).containsExactly("PERIODIC", "RETURN_FROM_HOSPITAL");
        assertThat(jdbc.queryForObject(
                        "SELECT source_id FROM reassessment_notice WHERE resident_id = ? AND basis ="
                                + " 'RETURN_FROM_HOSPITAL'",
                        Long.class,
                        id))
                .isEqualTo(historyId);
        assertThat(jdbc
                        .queryForList(
                                "SELECT deadline_from FROM reassessment_notice WHERE resident_id = ?",
                                java.sql.Timestamp.class,
                                id)
                        .stream()
                        .map(java.sql.Timestamp::toInstant))
                .allSatisfy(from -> assertThat(from).isCloseTo(returnedAt, within(5, ChronoUnit.SECONDS)));
        assertNear(status(id).dueAt(), returnedAt.plus(DEADLINE));
        assertThat(dueCount(id)).isEqualTo(2);
    }

    @Test
    void T048_recordReturnFromTemporaryAbsence_doesNotNotify() {
        long id = residentAssessedNow();
        moveTo(id, ResidentStatus.TEMPORARILY_ABSENT);

        user.set(office.user());
        statusService.recordReturn(target(id), reason(), noDetails());

        assertThat(noticeRows(id)).isZero();
    }

    @Test
    void US6_10_finalStatus_noMoreNoticesAlertsOrDueState() {
        for (ResidentStatus finalStatus :
                List.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED)) {
            long id = residentAssessedNow();
            clock.advanceBy(PERIOD.plusMinutes(1));
            notices.runPeriodic();
            assertThat(status(id).state()).isEqualTo(ReassessmentState.DUE);

            moveTo(id, finalStatus);
            clock.advanceBy(DEADLINE.plusMinutes(1));
            notices.runOverdue();
            int rowsBefore = noticeRows(id);

            assertThat(events.stream(ReassessmentOverdue.class).filter(e -> e.residentId() == id))
                    .as(finalStatus.name())
                    .isEmpty();
            assertThat(status(id).state()).isEqualTo(ReassessmentState.NOT_APPLICABLE);
            assertThat(notices.notifyAfterIncident(id, 900L)).isFalse();
            assertThat(noticeRows(id)).isEqualTo(rowsBefore);
        }
    }

    @Test
    void NFR_04_uniqueKeyBlocksADuplicateNoticeAtTheDatabase() {
        long id = residentAssessedNow();
        String insert = "INSERT INTO reassessment_notice (resident_id, basis, source_type, source_id, notified_at)"
                + " VALUES (?, 'AFTER_INCIDENT', 'INCIDENT', 77, ?)";
        jdbc.update(insert, id, java.sql.Timestamp.from(clock.instant()));

        assertThatThrownBy(() -> jdbc.update(insert, id, java.sql.Timestamp.from(clock.instant())))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
