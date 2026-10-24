package com.cambae.backend.m01.internal.consent.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.events.MissingConsentReminder;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Hành chính khi đã tiếp nhận mà chưa có bản đồng ý Hiệu lực (001 T058; FR-012, SC-013): mỗi CFG-M01-06 kể từ lúc
 * bắt đầu (Hoàn tất tiếp nhận thiếu bản đồng ý), cho tới khi có bản Hiệu lực hoặc hồ sơ sang trạng thái cuối. Chỉ phát
 * sự kiện và ghi bảng kỹ thuật {@code periodic_reminder}, không tạo bản ghi nghiệp vụ (000 FR-010a). Người nhận và mức
 * do 009 xếp.
 */
@Service
public class MissingConsentReminderService {

    public static final String JOB = "MISSING_CONSENT_REMINDER";
    static final String TYPE = "MISSING_CONSENT";

    private final JdbcTemplate jdbc;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    MissingConsentReminderService(
            JdbcTemplate jdbc, ParameterApi parameters, ApplicationEventPublisher events, BusinessTime time) {
        this.jdbc = jdbc;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Bắt đầu nhắc cho hồ sơ; gọi lại không thêm dòng (khóa chính chống lặp). */
    @Transactional
    public void startReminding(long residentId) {
        jdbc.update(
                "INSERT IGNORE INTO periodic_reminder (type, resident_id, milestone_id, started_at) VALUES (?, ?, 0, ?)",
                TYPE,
                residentId,
                utc(time.now()));
    }

    /** Số lần nhắc phát trong lần chạy này. */
    @Transactional
    public int run() {
        Duration period = parameters.read(ParameterCode.CFG_M01_06).asDuration();
        Instant now = time.now();
        List<long[]> due = jdbc.query(
                "SELECT r.resident_id, r.reminder_count FROM periodic_reminder r"
                        + " JOIN resident s ON s.id = r.resident_id"
                        + " WHERE r.type = ? AND s.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
                        + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED')"
                        + " AND COALESCE(r.last_reminded_at, r.started_at) <= ?"
                        + " AND NOT EXISTS (SELECT 1 FROM consent c WHERE c.resident_id = r.resident_id"
                        + " AND c.status = 'ACTIVE')",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)},
                TYPE,
                utc(now.minus(period)));
        for (long[] row : due) {
            int number = (int) row[1] + 1;
            jdbc.update(
                    "UPDATE periodic_reminder SET last_reminded_at = ?, reminder_count = ? WHERE type = ? AND"
                            + " resident_id = ? AND milestone_id = 0",
                    utc(now),
                    number,
                    TYPE,
                    row[0]);
            events.publishEvent(new MissingConsentReminder(row[0], number, now));
        }
        return due.size();
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
