package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeInput;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Nội dung thêm hoặc sửa loại sự cố; cờ cho phép không gắn NCT chỉ có ở phản hồi. Schema {@code IncidentType} dùng
 * chung cho đọc, ghi: {@code id} lấy theo đường dẫn, bỏ qua khi ghi.
 */
public record IncidentTypeRequest(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IncidentLevel defaultLevel,
        boolean nonHealth,
        Boolean active) {
    public IncidentTypeInput toInput() {
        return new IncidentTypeInput(code, name, defaultLevel, nonHealth, 0);
    }
}
