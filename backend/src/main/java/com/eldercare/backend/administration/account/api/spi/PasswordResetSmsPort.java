package com.eldercare.backend.administration.account.api.spi;

/**
 * SPI: gửi mã đặt lại mật khẩu qua tin nhắn (002 T105; FR-052a, Q-06, Q-399). Khai báo ở 002 để tránh vòng M15 ↔ M13;
 * M13 (009) cài bằng nhà cung cấp tin nhắn của mình. Không có bean thì chức năng coi như chưa cấu hình.
 */
public interface PasswordResetSmsPort {

    /** Kênh tin nhắn đã cấu hình nhà cung cấp (Q-06); {@code false} thì màn đăng nhập ẩn "Quên mật khẩu". */
    boolean available();

    /** Gửi một tin tới số trong hồ sơ; {@code true} khi nhà cung cấp nhận tin. Nội dung không ghi vào nhật ký gửi. */
    boolean send(String phoneNumber, String content);
}
