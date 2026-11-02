package com.eldercare.backend.administration.correction.web.response;

/** Người thực hiện là người dùng hay Bộ lập lịch (schema {@code Actor}). */
public enum ActorType {
    USER,
    SCHEDULER,
    // Công cụ khởi tạo Quản lý viện đầu tiên (002 BR-M15-22); chỉ nhật ký có
    BOOTSTRAP,
    // Lần đăng nhập thất bại, chưa xác định người (002 FR-015); chỉ nhật ký có
    ANONYMOUS
}
