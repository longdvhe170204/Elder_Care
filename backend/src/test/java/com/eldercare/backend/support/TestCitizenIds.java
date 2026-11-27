package com.eldercare.backend.support;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Số CCCD 12 chữ số chưa dùng cho fixture tạo hồ sơ (CCCD bắt buộc từ BF-01 2026-11-26). Schema test giữ dữ liệu qua các
 * lần chạy và CCCD của hồ sơ chưa kết thúc là duy nhất, nên số bắt đầu từ mili giây lúc nạp lớp: lần chạy sau luôn lớn hơn
 * các số lần trước đã dùng (mỗi lần dùng ít số hơn số mili giây giữa hai lần chạy). Mỗi JVM của failsafe có schema riêng.
 */
public final class TestCitizenIds {

    private static final long LIMIT = 1_000_000_000_000L;
    private static final AtomicLong NEXT = new AtomicLong(System.currentTimeMillis() % LIMIT);

    private TestCitizenIds() {}

    public static String next() {
        return String.format("%012d", NEXT.incrementAndGet() % LIMIT);
    }
}
