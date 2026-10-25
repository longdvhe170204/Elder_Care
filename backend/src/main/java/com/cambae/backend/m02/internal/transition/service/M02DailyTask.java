package com.cambae.backend.m02.internal.transition.service;

import java.time.LocalDate;

/**
 * Một việc của job hằng ngày {@code M02_DAILY} (research T-11; FR-077, BR-M02-19). Năm việc đăng ký dần ở các user
 * story: {@code CALCULATE_SCORES}, {@code ABSENCE_DAYS}, {@code CONTRACTS}, {@code ENDINGS}, {@code REMINDERS}. Thứ tự
 * chạy trong một ngày theo {@code @Order} của bean. Việc không tạo yêu cầu, chi phí hay hồ sơ (000 FR-010a).
 */
public interface M02DailyTask {

    /** Khóa của việc trong {@code daily_run.task}. */
    String name();

    /**
     * Việc chỉ có nghĩa với ngày mới nhất (tính điểm, nhắc gộp một lần): ngày bị lỡ chỉ ghi "bỏ qua" mà không chạy.
     */
    default boolean latestDayOnly() {
        return false;
    }

    /**
     * Chạy cho ngày {@code day}, trong transaction riêng của (việc, ngày); ném lỗi thì không ghi dòng {@code daily_run}
     * nên lần chạy sau làm lại từ ngày đó. Tham số CFG đọc giá trị hiện hành tại lúc chạy (000 FR-018).
     *
     * @return số bản ghi đã xử lý
     */
    int run(LocalDate day);
}
