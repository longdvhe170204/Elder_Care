package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeView;

/** Loại diễn biến trả về qua HTTP. */
public record ProgressionTypeResponse(
        long id, String code, String name, ProgressionGroup group, boolean resuscitation) {
    public static ProgressionTypeResponse from(ProgressionTypeView view) {
        return new ProgressionTypeResponse(view.id(), view.code(), view.name(), view.group(), view.resuscitation());
    }
}
