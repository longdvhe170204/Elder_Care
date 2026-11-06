package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.events.CareLevelChangeRequestReminder;
import com.eldercare.backend.resident.api.spi.CareLevelChangeRequestApi;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.persistence.AssessmentRepository;
import com.eldercare.backend.resident.internal.assessment.persistence.CareLevelHistoryStore;
import com.eldercare.backend.resident.internal.domain.DueReminder;
import com.eldercare.backend.resident.internal.persistence.PeriodicReminderStore;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Hành chính lập yêu cầu đổi mức chăm sóc (001 T064; FR-036, Q-318, research H-12): sau khi đánh giá lại ra mức
 * khác mức hiện hành, mỗi CFG-M03-02 nhắc một lần cho tới khi 004 có yêu cầu đổi mức chưa kết thúc, có lần đánh giá lại
 * mới (nhắc theo lần mới), mức hiện hành đã bằng mức mới, hoặc hồ sơ sang trạng thái cuối. Yêu cầu bị Từ chối không dừng
 * nhắc. Chỉ phát sự kiện và ghi bảng kỹ thuật {@code reminder_marker}, không tạo yêu cầu (Q-306). Chưa có bản 004 thì coi
 * như không có yêu cầu nào.
 */
@Service
public class CareLevelChangeRequestReminderService {

    public static final String JOB = "REMIND_CARE_LEVEL_CHANGE_REQUEST";
    static final String TYPE = "CREATE_CARE_LEVEL_CHANGE_REQUEST";

    private final PeriodicReminderStore reminders;
    private final ParameterApi parameters;
    private final ObjectProvider<CareLevelChangeRequestApi> requests;
    private final AssessmentRepository assessments;
    private final AssessmentContents contents;
    private final CareLevelHistoryStore history;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    CareLevelChangeRequestReminderService(
            PeriodicReminderStore reminders,
            ParameterApi parameters,
            ObjectProvider<CareLevelChangeRequestApi> requests,
            AssessmentRepository assessments,
            AssessmentContents contents,
            CareLevelHistoryStore history,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.reminders = reminders;
        this.parameters = parameters;
        this.requests = requests;
        this.assessments = assessments;
        this.contents = contents;
        this.history = history;
        this.events = events;
        this.time = time;
    }

    /** Bắt đầu nhắc cho lần đánh giá lại vừa xác nhận ra mức khác; gọi lại không thêm dòng. */
    @Transactional
    public void startReminding(long residentId, long assessmentId) {
        reminders.start(TYPE, residentId, assessmentId);
    }

    /** Số lần nhắc phát trong lần chạy này. */
    @Transactional
    public int run() {
        Duration period = parameters.read(ParameterCode.CFG_M03_02).asDuration();
        Instant now = time.now();
        CareLevelChangeRequestApi api = requests.getIfAvailable();
        int reminded = 0;
        for (DueReminder row : reminders.due(TYPE, now.minus(period))) {
            long residentId = row.residentId();
            long assessmentId = row.milestoneId();
            var latest = assessments.findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
                    residentId, AssessmentStatus.CONFIRMED);
            if (latest.isEmpty() || latest.get().getId() != assessmentId) {
                continue;
            }
            CareLevel newLevel = contents.of(latest.get()).careLevel();
            if (history.current(residentId).map(current -> current == newLevel).orElse(false)
                    || (api != null && api.hasOpenRequest(residentId))) {
                continue;
            }
            int number = row.reminderCount() + 1;
            reminders.markReminded(TYPE, residentId, assessmentId, number);
            events.publishEvent(new CareLevelChangeRequestReminder(residentId, assessmentId, newLevel, number, now));
            reminded++;
        }
        return reminded;
    }
}
