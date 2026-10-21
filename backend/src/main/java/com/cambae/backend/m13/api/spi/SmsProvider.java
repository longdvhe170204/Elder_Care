package com.cambae.backend.m13.api.spi;

/**
 * Cổng tới nhà cung cấp tin nhắn ({@code NhaCungCapTinNhan}; research B-05). Mặc định "chưa cấu hình" (Q-06); bộ chuyển
 * cho nhà cung cấp cụ thể viết khi chốt (T026a). Chỉ M13 gọi.
 */
public interface SmsProvider {

    SmsResult send(String phoneNumber, String content);
}
