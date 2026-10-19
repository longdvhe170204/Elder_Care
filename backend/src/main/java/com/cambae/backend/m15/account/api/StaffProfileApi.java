package com.cambae.backend.m15.account.api;

import com.cambae.backend.common.Role;
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
}
