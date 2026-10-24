package com.cambae.backend.m01.internal.wish.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.events.MissingEndOfLifeWishReminder;
import com.cambae.backend.m01.api.events.SelfDeterminationReviewReminder;
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
 * Hai việc nhắc của phiếu nguyện vọng cuối đời (001 T069; research H-12). Nhắc thiếu phiếu (FR-023d): mỗi CFG-M01-06 kể
 * từ lúc Hoàn tất tiếp nhận thiếu phiếu, cho tới khi có phiếu Hiệu lực hoặc hồ sơ sang trạng thái cuối (FR-023f). Nhắc xem
 * lại nhận định (FR-023i, Q-360): một lần khi tới ngày xem lại của phiếu Hiệu lực. Chỉ phát sự kiện và ghi bảng kỹ thuật
 * {@code periodic_reminder}, không tạo bản ghi nghiệp vụ và không đổi phiếu (000 FR-010a); người nhận và mức do 009 xếp.
 * Phiếu nguyện vọng khi điều trị không có nhắc (FR-023l).
 */
@Service
public class WishReminderService {

    static final String MISSING = "MISSING_END_OF_LIFE_WISH";
    static final String REVIEW = "REVIEW_SELF_DETERMINATION";

    private final JdbcTemplate jdbc;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    WishReminderService(
            JdbcTemplate jdbc, ParameterApi parameters, ApplicationEventPublisher events, BusinessTime time) {
        this.jdbc = jdbc;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Bắt đầu nhắc thiếu phiếu cho hồ sơ; gọi lại không thêm dòng (khóa chính chống lặp). */
    @Transactional
    public void startReminding(long residentId) {
        jdbc.update(
                "INSERT IGNORE INTO periodic_reminder (type, resident_id, milestone_id, started_at) VALUES (?, ?, 0, ?)",
                MISSING,
                residentId,
                utc(time.now()));
    }

    /** Số lần nhắc thiếu phiếu phát trong lần chạy này. */
    @Transactional
    public int runMissing() {
        Duration period = parameters.read(ParameterCode.CFG_M01_06).asDuration();
        Instant now = time.now();
        List<long[]> due = jdbc.query(
                "SELECT r.resident_id, r.reminder_count FROM periodic_reminder r"
                        + " JOIN resident s ON s.id = r.resident_id"
                        + " WHERE r.type = ? AND s.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
                        + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED')"
                        + " AND COALESCE(r.last_reminded_at, r.started_at) <= ?"
                        + " AND NOT EXISTS (SELECT 1 FROM end_of_life_wish w WHERE w.resident_id = r.resident_id"
                        + " AND w.status = 'ACTIVE')",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)},
                MISSING,
                utc(now.minus(period)));
        for (long[] row : due) {
            int number = (int) row[1] + 1;
            jdbc.update(
                    "UPDATE periodic_reminder SET last_reminded_at = ?, reminder_count = ? WHERE type = ? AND"
                            + " resident_id = ? AND milestone_id = 0",
                    utc(now),
                    number,
                    MISSING,
                    row[0]);
            events.publishEvent(new MissingEndOfLifeWishReminder(row[0], number));
        }
        return due.size();
    }

    /** Số lần nhắc xem lại nhận định phát trong lần chạy này. */
    @Transactional
    public int runReviews() {
        Instant now = time.now();
        List<long[]> due = jdbc.query(
                "SELECT w.resident_id, w.id FROM end_of_life_wish w JOIN resident s ON s.id = w.resident_id"
                        + " WHERE w.status = 'ACTIVE' AND w.review_date <= ?"
                        + " AND s.status NOT IN ('RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED')"
                        + " AND NOT EXISTS (SELECT 1 FROM periodic_reminder r WHERE r.type = ?"
                        + " AND r.resident_id = w.resident_id AND r.milestone_id = w.id)",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)},
                time.today(),
                REVIEW);
        int sent = 0;
        for (long[] row : due) {
            // khóa chính (type, resident_id, milestone_id) chặn nhắc trùng khi hai lần chạy chồng nhau
            int inserted = jdbc.update(
                    "INSERT IGNORE INTO periodic_reminder (type, resident_id, milestone_id, started_at,"
                            + " last_reminded_at, reminder_count) VALUES (?, ?, ?, ?, ?, 1)",
                    REVIEW,
                    row[0],
                    row[1],
                    utc(now),
                    utc(now));
            if (inserted == 1) {
                events.publishEvent(new SelfDeterminationReviewReminder(row[0], row[1]));
                sent++;
            }
        }
        return sent;
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
