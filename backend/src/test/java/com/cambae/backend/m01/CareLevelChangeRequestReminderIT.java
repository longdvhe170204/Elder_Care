package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.CareLevelChangeRequestReminder;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import com.cambae.backend.m01.internal.assessment.service.CareLevelChangeRequestReminderService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T061 (US6 #5; FR-036, Q-318; đồng hồ giả lập): sau khi đánh giá lại ra mức khác mức hiện hành, Hành chính được
 * nhắc lập yêu cầu đổi mức mỗi CFG-M03-02 (mặc định 1 ngày) tới khi 004 có yêu cầu chưa kết thúc, hoặc có lần đánh giá
 * lại mới, hoặc mức hiện hành đã bằng mức mới, hoặc hồ sơ sang trạng thái cuối. Yêu cầu bị Từ chối không dừng nhắc.
 */
class CareLevelChangeRequestReminderIT extends StatusTestBase {

    private static final Duration PERIOD = Duration.ofDays(1);

    @Autowired
    TestClockControl clock;

    @Autowired
    CareLevelChangeRequestReminderService reminders;

    @Autowired
    CareLevelApi careLevels;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    /** Hồ sơ Đang lưu trú, mức hiện hành "thường xuyên"; đã xác nhận một đánh giá lại ra mức "đặc biệt". */
    private long residentNeedingRequest() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        confirmed(id, AssessmentType.INITIAL, CareLevel.REGULAR, "Đầu vào", null);
        moveTo(id, ResidentStatus.IN_RESIDENCE);
        confirmed(id, AssessmentType.REASSESSMENT, CareLevel.SPECIAL, "Nặng hơn", null);
        return id;
    }

    private List<Integer> numbers(long residentId) {
        return events.stream(CareLevelChangeRequestReminder.class)
                .filter(e -> e.residentId() == residentId)
                .map(CareLevelChangeRequestReminder::reminderNumber)
                .toList();
    }

    private void passOnePeriod() {
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();
    }

    @Test
    void US6_5_remindsEveryPeriod_numberedFromOne_withTheNewLevel() {
        long id = residentNeedingRequest();

        clock.advanceBy(PERIOD.minusMinutes(1));
        reminders.run();
        assertThat(numbers(id)).as("chưa tới một chu kỳ").isEmpty();

        clock.advanceBy(Duration.ofMinutes(2));
        reminders.run();
        reminders.run();
        assertThat(numbers(id)).as("chạy lại không nhắc trùng").containsExactly(1);

        passOnePeriod();
        assertThat(numbers(id)).containsExactly(1, 2);
        assertThat(events.stream(CareLevelChangeRequestReminder.class).filter(e -> e.residentId() == id))
                .allSatisfy(e -> assertThat(e.newLevel()).isEqualTo(CareLevel.SPECIAL));
        assertThat(careLevels.current(id)).as("mức hiện hành giữ nguyên").contains(CareLevel.REGULAR);
    }

    @Test
    void US6_5_noReminderWhenTheNewLevelEqualsTheCurrentOne() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        confirmed(id, AssessmentType.INITIAL, CareLevel.REGULAR, "Đầu vào", null);
        moveTo(id, ResidentStatus.IN_RESIDENCE);
        confirmed(id, AssessmentType.REASSESSMENT, CareLevel.REGULAR, "Như cũ", null);

        passOnePeriod();

        assertThat(numbers(id)).isEmpty();
    }

    @Test
    void Q_318_stopsWhileAnOpenRequestExists_andResumesAfterItIsRejected() {
        long id = residentNeedingRequest();
        passOnePeriod();
        assertThat(numbers(id)).containsExactly(1);

        changeRequests.open(id, 901L);
        passOnePeriod();
        assertThat(numbers(id)).as("đã có yêu cầu chưa kết thúc").containsExactly(1);

        changeRequests.supersedeByAssessment(id, 0L);
        passOnePeriod();
        assertThat(numbers(id)).as("yêu cầu bị Từ chối: nhắc tiếp").containsExactly(1, 2);
    }

    @Test
    void FR_036_stopsOnceTheRequestIsAppliedOrANewReassessmentIsConfirmed() {
        long applied = residentNeedingRequest();
        passOnePeriod();
        inTransaction(() -> {
            user.set(office.user());
            careLevels.applyChange(applied, CareLevel.SPECIAL, LocalDate.now(), 902L);
            return null;
        });
        passOnePeriod();
        assertThat(numbers(applied)).as("mức hiện hành đã bằng mức mới").containsExactly(1);

        long replaced = residentNeedingRequest();
        passOnePeriod();
        AssessmentView next = confirmed(replaced, AssessmentType.REASSESSMENT, CareLevel.BASIC, "Khỏe lại", null);
        assertThat(next.careLevel()).isEqualTo(CareLevel.BASIC);
        int before = numbers(replaced).size();
        passOnePeriod();
        List<CareLevel> levels = events.stream(CareLevelChangeRequestReminder.class)
                .filter(e -> e.residentId() == replaced)
                .map(CareLevelChangeRequestReminder::newLevel)
                .toList();
        assertThat(levels.subList(0, before)).containsOnly(CareLevel.SPECIAL);
        assertThat(levels.subList(before, levels.size()))
                .as("lần đánh giá lại mới thay lần cũ: nhắc theo mức mới")
                .containsOnly(CareLevel.BASIC);
    }

    @Test
    void US6_10_stopsWhenTheProfileReachesAFinalStatus() {
        long id = residentNeedingRequest();
        passOnePeriod();

        moveTo(id, ResidentStatus.RESIDENCY_ENDED);
        passOnePeriod();

        assertThat(numbers(id)).containsExactly(1);
    }
}
