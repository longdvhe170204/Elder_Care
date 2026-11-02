package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeInput;

/** Nội dung thêm hoặc sửa loại sự cố; cờ cho phép không gắn NCT chỉ có ở phản hồi. */
public record IncidentTypeRequest(
        String code, String name, IncidentLevel defaultLevel, boolean nonHealth, Boolean active) {
    public IncidentTypeInput toInput() {
        return new IncidentTypeInput(code, name, defaultLevel, nonHealth, 0);
    }
}
