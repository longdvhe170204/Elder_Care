package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.ReassessmentApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ReassessmentState;
import com.eldercare.backend.resident.api.dto.ReassessmentStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ReassessmentDue;
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

/**
 * 001 T060 (US6 #1 → #4, #10; FR-039 → FR-043, BR-M01-02; đồng hồ giả lập): báo đánh giá lại định kỳ khi lần Đã xác nhận
 * gần nhất qua CFG-M01-02 (mặc định 90 ngày), báo sau sự cố một lần cho mỗi sự cố, hạn CFG-M01-03 (mặc định 48 giờ) tính
 * từ lần báo sớm nhất chưa đáp ứng, cảnh báo quá hạn một lần mỗi lần quá hạn, hồ sơ Điều trị tại bệnh viện chưa tính hạn
 * tới khi Ghi nhận trở về (cùng căn cứ {@code RETURN_FROM_HOSPITAL}, T048), hồ sơ ở trạng thái cuối không báo. Báo là
 * bảng kỹ thuật {@code reassessment_notice}, không tạo đánh giá hay bản ghi yêu cầu nào.
 */
class BR_05_ReassessmentNoticeIT extends StatusTestBase {

    @Autowired
    com.eldercare.backend.resident.api.ResidentStatisticsApi statistics;

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
                "SELECT basis FROM " + com.eldercare.backend.support.ResidentHistoryRows.NOTICES
                        + " WHERE resident_id = ? ORDER BY id",
                String.class,
                residentId);
    }

    /** Cột DATETIME(6) làm tròn tới micro giây nên nới mỗi đầu 1 micro giây. */
    private static void assertBetween(Instant actual, Instant from, Instant to) {
        assertThat(actual).isBetween(from.minus(1, ChronoUnit.MICROS), to.plus(1, ChronoUnit.MICROS));
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
                "SELECT COUNT(*) FROM " + com.eldercare.backend.support.ResidentHistoryRows.NOTICES
                        + " WHERE resident_id = ?",
                Integer.class,
                residentId);
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
}
