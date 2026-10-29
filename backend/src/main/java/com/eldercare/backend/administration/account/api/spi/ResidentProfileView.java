package com.eldercare.backend.administration.account.api.spi;

/**
 * Phần hồ sơ người cao tuổi tài khoản cần.
 *
 * @param inResidence trạng thái Đang lưu trú (FR-067)
 * @param finalStatus đã ở trạng thái cuối: Kết thúc lưu trú, Qua đời, Hủy tiếp nhận (FR-072 (a), FR-073)
 */
public record ResidentProfileView(String fullName, boolean inResidence, boolean finalStatus) {}
