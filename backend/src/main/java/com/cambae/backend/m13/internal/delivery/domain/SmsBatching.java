package com.cambae.backend.m13.internal.delivery.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Khung gộp tin nhắn Trung bình ({@code KhungGop}; FR-016b, Q-101, research B-05): khung dài cố định CFG-M13-03, không
 * kéo dài bởi tin mới; tin tổng hợp chỉ có số thông báo và lời mời mở ứng dụng, không có họ tên, sức khỏe.
 */
public final class SmsBatching {

    private SmsBatching() {}

    /** Dồn vào khung đang mở ({@code openWindowEnd} sau bây giờ); không có khung mở thì gửi ngay và mở khung. */
    public static boolean batch(Optional<Instant> openWindowEnd, Instant now) {
        return openWindowEnd.filter(end -> end.isAfter(now)).isPresent();
    }

    public static String digest(int count) {
        return "Bạn có " + count + " thông báo mới. Vui lòng mở ứng dụng để xem.";
    }
}
