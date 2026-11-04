package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.MissingEndOfLifeWishReminder;
import com.eldercare.backend.resident.api.events.SelfDeterminationReviewReminder;
import com.eldercare.backend.resident.internal.domain.DueReminder;
import com.eldercare.backend.resident.internal.persistence.PeriodicReminderStore;
import com.eldercare.backend.resident.internal.wish.domain.WishReviewDue;
import com.eldercare.backend.resident.internal.wish.persistence.EndOfLifeWishStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hai việc nhắc của phiếu nguyện vọng cuối đời (001 T069; research H-12). Nhắc thiếu phiếu (FR-023d): mỗi CFG-M01-06 kể
 * từ lúc Hoàn tất tiếp nhận thiếu phiếu, cho tới khi có phiếu Hiệu lực hoặc hồ sơ sang trạng thái cuối (FR-023f). Nhắc xem
 * lại nhận định (FR-023i, Q-360): một lần khi tới ngày xem lại của phiếu Hiệu lực. Chỉ phát sự kiện và ghi bảng kỹ thuật
 * {@code reminder_marker}, không tạo bản ghi nghiệp vụ và không đổi phiếu (000 FR-010a); người nhận và mức do 009 xếp.
 * Phiếu nguyện vọng khi điều trị không có nhắc (FR-023l).
 */
@Service
public class WishReminderService {

    static final String MISSING = "MISSING_END_OF_LIFE_WISH";
    static final String REVIEW = "REVIEW_SELF_DETERMINATION";

    private final PeriodicReminderStore reminders;
    private final EndOfLifeWishStore wishes;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    private final AccountApi accounts;

    WishReminderService(
            PeriodicReminderStore reminders,
            EndOfLifeWishStore wishes,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time,
            AccountApi accounts) {
        this.accounts = accounts;
        this.reminders = reminders;
        this.wishes = wishes;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Bắt đầu nhắc thiếu phiếu cho hồ sơ; gọi lại không thêm dòng (khóa chính chống lặp). */
    @Transactional
    public void startReminding(long residentId) {
        reminders.start(MISSING, residentId, 0);
    }

    /** Số lần nhắc thiếu phiếu phát trong lần chạy này. */
    @Transactional
    public int runMissing() {
        Duration period = parameters.read(ParameterCode.CFG_M01_06).asDuration();
        Instant now = time.now();
        List<DueReminder> due = reminders.dueWithoutActive(
                MISSING, "(SELECT resident_id, status FROM wish WHERE kind = 'END_OF_LIFE')", now.minus(period));
        for (DueReminder row : due) {
            int number = row.reminderCount() + 1;
            reminders.markReminded(MISSING, row.residentId(), 0, number);
            events.publishEvent(new MissingEndOfLifeWishReminder(row.residentId(), number, now));
        }
        return due.size();
    }

    /** Số lần nhắc xem lại nhận định phát trong lần chạy này. */
    @Transactional
    public int runReviews() {
        Instant now = time.now();
        int sent = 0;
        for (WishReviewDue row : wishes.dueReviews(time.today(), REVIEW)) {
            // khóa duy nhất của reminder_marker chặn nhắc trùng khi hai lần chạy chồng nhau
            if (reminders.startReminded(REVIEW, row.residentId(), row.wishId())) {
                events.publishEvent(new SelfDeterminationReviewReminder(
                        row.residentId(), row.wishId(), staffIdOf(row.assessingDoctorId()), now));
                sent++;
            }
        }
        return sent;
    }

    /** Hồ sơ nhân viên của tài khoản Bác sĩ đã nhận định; không rõ thì báo Bác sĩ trực (FR-023i). */
    private Long staffIdOf(long accountId) {
        return accounts.byId(accountId).map(AccountView::staffId).orElse(null);
    }
}
