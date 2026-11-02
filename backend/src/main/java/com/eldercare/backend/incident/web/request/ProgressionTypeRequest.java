package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeInput;

/** Nội dung thêm loại diễn biến. */
public record ProgressionTypeRequest(String code, String name, ProgressionGroup group, boolean resuscitation) {
    public ProgressionTypeInput toInput() {
        return new ProgressionTypeInput(code, name, group, resuscitation, 0);
    }
}
