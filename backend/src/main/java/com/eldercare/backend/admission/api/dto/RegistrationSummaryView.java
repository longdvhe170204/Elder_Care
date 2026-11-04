package com.eldercare.backend.admission.api.dto;

/**
 * Số liệu khai báo tạm trú, lưu trú cho dashboard của 016 (FR-011, 018.5), đếm theo người cao tuổi tại lúc hỏi: {@code
 * toCreate} chưa lập khai báo hoặc khai báo ở Cần khai báo, chưa quá hạn; {@code overdue} quá hạn khai báo; {@code expired}
 * có khai báo Hết hạn; {@code toDeregister} cần khai báo xóa.
 */
public record RegistrationSummaryView(int toCreate, int overdue, int expired, int toDeregister) {}
