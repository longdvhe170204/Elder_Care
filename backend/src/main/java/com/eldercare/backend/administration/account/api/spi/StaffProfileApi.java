package com.eldercare.backend.administration.account.api.spi;

import com.eldercare.backend.common.Role;
import java.time.LocalDate;
import java.util.Optional;

/**
 * SPI: hồ sơ nhân viên cho tạo, khóa, mở khóa tài khoản (FR-004, FR-004a, FR-011). Khai báo ở 002 để tránh vòng M15 ↔
 * M09; M09 (008) cài (008 contracts mục 1.1).
 */
public interface StaffProfileApi {

    StaffStatus status(long staffId, LocalDate date);

    /** Vai trò hệ thống ứng với chức danh; rỗng khi chức danh không dùng hệ thống (FR-004a). */
    Optional<Role> positionRole(long staffId);

    /**
     * Tạo hồ sơ nhân viên tối thiểu (họ tên, chức danh ứng với vai trò) cho công cụ khởi tạo Quản lý viện đầu tiên;
     * trả id hồ sơ (002 T016; BR-M15-22, FR-004b). Chạy trong transaction của lệnh gọi.
     */
    long createMinimal(String fullName, Role positionRole);

    /** Họ tên trên hồ sơ nhân viên, cho {@code AccountApi.fullName} (002 T032a). */
    Optional<String> fullName(long staffId);

    /**
     * Số điện thoại liên hệ trên hồ sơ nhân viên, cho tin nhắn của 009 (FR-016, FR-019; luật "Đổi api", chủ dự án duyệt
     * 2026-10-23); rỗng khi hồ sơ không có số.
     */
    Optional<String> phoneNumber(long staffId);
}
