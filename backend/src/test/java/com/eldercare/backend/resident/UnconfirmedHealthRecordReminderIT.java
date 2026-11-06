package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.events.UnconfirmedHealthRecordReminder;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService;
import com.eldercare.backend.resident.internal.health.service.UnconfirmedHealthRecordReminderService;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T105 (Q-400, BR-M01-17; đồng hồ giả lập): Hoàn tất tiếp nhận khi hồ sơ sức khỏe ban đầu chưa Bác sĩ xác nhận hoặc còn
 * mục tự khai chưa kiểm chứng thì nhắc mỗi CFG-M01-06 kể từ lúc bắt đầu, cho tới khi hồ sơ được xác nhận và hết mục chưa
 * kiểm chứng. Người nhận (Bác sĩ, Điều dưỡng phụ trách) và mức do sự kiện khai báo cho 009.
 */
class UnconfirmedHealthRecordReminderIT extends StatusTestBase {

    private static final Duration PERIOD = Duration.ofDays(1);
    private static final InitialHealthRecordContent CONTENT =
            new InitialHealthRecordContent("O+", "Đi lại khó", null, null, null, null, "Thuốc A", null);

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @Autowired
    UnconfirmedHealthRecordReminderService reminders;

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    HealthItemService items;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private long completedAdmissionWithoutConfirmedRecord() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        user.set(office.user());
        statusService.completeAdmission(target(id), reason(), noDetails());
        return id;
    }

    private long count(long residentId) {
        return events.stream(UnconfirmedHealthRecordReminder.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    private void confirmRecord(long id) {
        user.set(doctor.user());
        var draft = records.saveDraft(target(id), null, CONTENT);
        records.confirm(target(id), "\"" + draft.version() + "\"");
    }

    @Test
    void T105_remindsEveryPeriodAfterStart_numberingTheReminders() {
        long id = completedAdmissionWithoutConfirmedRecord();

        reminders.run();
        assertThat(count(id)).as("chưa tới một chu kỳ").isZero();

        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();
        reminders.run();
        assertThat(count(id)).as("chạy lại không nhắc thêm").isEqualTo(1);

        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();
        assertThat(events.stream(UnconfirmedHealthRecordReminder.class)
                        .filter(e -> e.residentId() == id)
                        .map(UnconfirmedHealthRecordReminder::reminderNumber)
                        .toList())
                .containsExactly(1, 2);
    }

    @Test
    void T105_stopsOnceRecordIsConfirmedAndNothingIsUnverified() {
        long id = completedAdmissionWithoutConfirmedRecord();
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();
        assertThat(count(id)).isEqualTo(1);

        confirmRecord(id);
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        assertThat(count(id)).as("đã xác nhận, không còn mục chưa kiểm chứng").isEqualTo(1);
    }

    @Test
    void T105_keepsRemindingWhileAnUnverifiedItemRemains_evenIfRecordIsConfirmed() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        confirmRecord(id);
        user.set(office.user());
        items.record(
                target(id),
                new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen suyễn", null, sourceId(), false));
        statusService.completeAdmission(target(id), reason(), noDetails());

        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        assertThat(count(id)).isEqualTo(1);
    }

    @Test
    void T105_confirmedAndVerifiedAtCompletion_neverStartsReminding() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        confirmRecord(id);
        user.set(office.user());
        statusService.completeAdmission(target(id), reason(), noDetails());

        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        assertThat(count(id)).isZero();
    }
}
