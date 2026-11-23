package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.ConversionRequest;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh Chuyển sự cố, schema {@code IncidentFromAlert} của api-007. */
public record ConvertToIncidentRequest(
        Long incidentTypeId,
        String description,
        String initialResponse,

        @Schema(description = "Bắt buộc khi cảnh báo chưa gắn người cao tuổi (FR-047i)")
        Long residentId,

        @Schema(
                types = {"boolean", "null"},
                description = "Đã thấy sự cố Khẩn cấp đang mở và chọn tạo sự cố riêng (FR-041)")
        Boolean createDespiteOpen,

        @Schema(
                types = {"integer", "null"},
                description = "Cảnh báo cùng vụ: gắn vào sự cố đang mở này, không tạo sự cố mới (FR-029)")
        Long existingIncidentId) {
    public ConversionRequest toConversion() {
        return new ConversionRequest(
                incidentTypeId,
                description,
                initialResponse,
                residentId,
                Boolean.TRUE.equals(createDespiteOpen),
                existingIncidentId);
    }
}
