package com.eldercare.backend.admission.internal.transition.service;

import java.time.LocalDate;

/**
 * Một việc của job hằng ngày {@code M02_DAILY} (research T-11; FR-077, BR-M02-19). Bốn việc đăng ký dần ở các user
 * story: {@code ABSENCE_NOTICE}, {@code CONTRACTS}, {@code ENDINGS}, {@code REMINDERS} (Q-394, Q-395 bỏ tính điểm, ngày vắng). Thứ tự
 * chạy trong một ngày theo {@code @Order} của bean. Việc không tạo yêu cầu, chi phí hay hồ sơ (000 FR-010a).
 */
public interface M02DailyTask {

    /** Khóa của việc trong dấu {@code M02.DAILY_RUN}. */
    String name();

    /**
     * Việc chỉ có nghĩa với ngày mới nhất (nhắc gộp một lần): ngày bị lỡ chỉ ghi "bỏ qua" mà không chạy.
     */
    default boolean latestDayOnly() {
        return false;
    }

    /**
     * Chạy cho ngày {@code day}, trong transaction riêng của (việc, ngày); ném lỗi thì không giữ dấu ngày
     * nên lần chạy sau làm lại từ ngày đó. Tham số CFG đọc giá trị hiện hành tại lúc chạy (000 FR-018).
     *
     * @return số bản ghi đã xử lý
     */
    int run(LocalDate day);
}
