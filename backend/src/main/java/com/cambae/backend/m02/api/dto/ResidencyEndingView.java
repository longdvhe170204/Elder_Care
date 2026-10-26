package com.cambae.backend.m02.api.dto;

import java.time.LocalDate;

/**
 * Hồ sơ kết thúc lưu trú Đang chuẩn bị cho 017 (FR-062, FR-066a). {@code overdueSince} rỗng khi chưa mang dấu "quá ngày
 * dự kiến". Provisional: danh sách trường chờ 017 xác nhận khi dùng.
 */
public record ResidencyEndingView(
        long id, long residentId, EndCase endCase, LocalDate expectedEndDate, LocalDate overdueSince) {}
