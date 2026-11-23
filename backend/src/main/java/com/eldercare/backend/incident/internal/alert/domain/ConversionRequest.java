package com.eldercare.backend.incident.internal.alert.domain;

/**
 * Nội dung lệnh Chuyển sự cố, Kích hoạt khẩn cấp từ cảnh báo (FR-029, FR-041). {@code existingIncidentId}: cảnh báo cùng vụ
 * với sự cố đang mở thì gắn vào sự cố đó, không tạo mới. {@code createDespiteOpen}: khác vụ thì tạo sự cố riêng dù người
 * cao tuổi đang có sự cố Khẩn cấp mở. {@code location}: nơi xảy ra của sự cố mới (màn điền sẵn từ yêu cầu hỗ trợ).
 * {@code addHelpers}: người đã bấm "Tôi đến giúp" thành người tham gia xử lý sự cố (đề xuất 2026-11-23).
 */
public record ConversionRequest(
        Long incidentTypeId,
        String description,
        String initialResponse,
        Long residentId,
        boolean createDespiteOpen,
        Long existingIncidentId,
        String location,
        boolean addHelpers) {
    public ConversionRequest(Long incidentTypeId, String description, String initialResponse, Long residentId) {
        this(incidentTypeId, description, initialResponse, residentId, false, null);
    }

    public ConversionRequest(
            Long incidentTypeId,
            String description,
            String initialResponse,
            Long residentId,
            boolean createDespiteOpen,
            Long existingIncidentId) {
        this(
                incidentTypeId,
                description,
                initialResponse,
                residentId,
                createDespiteOpen,
                existingIncidentId,
                null,
                true);
    }
}
