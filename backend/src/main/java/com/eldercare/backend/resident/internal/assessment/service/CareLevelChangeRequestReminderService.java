package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.events.CareLevelChangeRequestReminder;
import com.eldercare.backend.resident.api.spi.CareLevelChangeRequestApi;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.persistence.AssessmentRepository;
import com.eldercare.backend.resident.internal.assessment.persistence.CareLevelHistoryStore;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Hành chính lập yêu cầu đổi mức chăm sóc (001 T064; FR-036, Q-318, research H-12): sau khi đánh giá lại ra mức
 * khác mức hiện hành, mỗi CFG-M03-02 nhắc một lần cho tới khi 004 có yêu cầu đổi mức chưa kết thúc, có lần đánh giá lại
 * mới (nhắc theo lần mới), mức hiện hành đã bằng mức mới, hoặc hồ sơ sang trạng thái cuối. Yêu cầu bị Từ chối không dừng
 * nhắc. Chỉ phát sự kiện và ghi bảng kỹ thuật {@code periodic_reminder}, không tạo yêu cầu (Q-306). Chưa có bản 004 thì coi
 * như không có yêu cầu nào.
 */
@Service
public class CareLevelChangeRequestReminderService {

    public static final String JOB = "REMIND_CARE_LEVEL_CHANGE_REQUEST";
    static final String TYPE = "CREATE_CARE_LEVEL_CHANGE_REQUEST";

    private final JdbcTemplate jdbc;
    private final ParameterApi parameters;
    private final ObjectProvider<CareLevelChangeRequestApi> requests;
    private final AssessmentRepository assessments;
    private final AssessmentContents contents;
    private final CareLevelHistoryStore history;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    CareLevelChangeRequestReminderService(
            JdbcTemplate jdbc,
            ParameterApi parameters,
            ObjectProvider<CareLevelChangeRequestApi> requests,
            AssessmentRepository assessments,
            AssessmentContents contents,
            CareLevelHistoryStore history,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.jdbc = jdbc;
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
        jdbc.update(
                "INSERT IGNORE INTO periodic_reminder (type, resident_id, milestone_id, started_at) VALUES (?, ?, ?, ?)",
                TYPE,
                residentId,
                assessmentId,
                utc(time.now()));
    }

    /** Số lần nhắc phát trong lần chạy này. */
    @Transactional
    public int run() {
        Duration period = parameters.read(ParameterCode.CFG_M03_02).asDuration();
        Instant now = time.now();
        CareLevelChangeRequestApi api = requests.getIfAvailable();
        List<long[]> due = jdbc.query(
                "SELECT r.resident_id, r.milestone_id, r.reminder_count FROM periodic_reminder r"
                        + " JOIN resident s ON s.id = r.resident_id"
                        + " WHERE r.type = ? AND s.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
                        + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED')"
                        + " AND COALESCE(r.last_reminded_at, r.started_at) <= ?",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2), rs.getLong(3)},
                TYPE,
                Timestamp.from(now.minus(period)));
        int reminded = 0;
        for (long[] row : due) {
            long residentId = row[0];
            long assessmentId = row[1];
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
            int number = (int) row[2] + 1;
            jdbc.update(
                    "UPDATE periodic_reminder SET last_reminded_at = ?, reminder_count = ? WHERE type = ? AND"
                            + " resident_id = ? AND milestone_id = ?",
                    Timestamp.from(now),
                    number,
                    TYPE,
                    residentId,
                    assessmentId);
            events.publishEvent(new CareLevelChangeRequestReminder(residentId, assessmentId, newLevel, number));
            reminded++;
        }
        return reminded;
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
