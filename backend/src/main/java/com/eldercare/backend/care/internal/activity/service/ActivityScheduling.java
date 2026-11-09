package com.eldercare.backend.care.internal.activity.service;

import java.time.LocalDate;
import java.util.*;

/**
 * 014 T005 (research L-15): việc định kỳ của 014 là bước {@code CareMinuteWork}/{@code CareDailyWork} trong khung
 * {@code CARE_MINUTE}/{@code CARE_DAILY} của 005 (ShedLock, chạy bù lúc khởi động, mốc theo (việc, ngày) ở
 * {@code reminder_marker}), không thêm job riêng (plan 014 Phase 2). Tên bước có tiền tố {@link #PREFIX}.
 */
public final class ActivityScheduling {
    public static final String PREFIX = "M04HD_";

    private ActivityScheduling() {}

    /**
     * Ngày cần xử lý "hết ngày": từ ngày cũ nhất còn buổi chưa đóng (lấy từ dữ liệu thật) tới hôm qua; không có thì chỉ
     * hôm qua. Ngày đã chạy bị mốc của {@code CareDailyWorkRunner} bỏ qua.
     */
    public static List<LocalDate> dueDates(LocalDate today, Optional<LocalDate> oldestOpen) {
        LocalDate yesterday = today.minusDays(1);
        LocalDate from = oldestOpen.filter(d -> d.isBefore(yesterday)).orElse(yesterday);
        return from.datesUntil(today).toList();
    }
}
