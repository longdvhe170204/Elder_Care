package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.catalog.domain.TaskTypeAttributes;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/** Cùng hình dạng hợp đồng TaskType;id/status do lệnh và vòng đời quản lý. */
@Schema(name = "TaskType")
public record TaskTypeWrite(
        Long id,
        String name,
        String group,
        boolean professional,
        String resultType,
        Role defaultRole,
        String defaultImportance,
        Integer windowBeforeMinutes,
        Integer windowAfterMinutes,
        boolean billable,
        Long serviceId,
        Integer upperLimit,
        String vitalSignType,
        Set<String> certificates,
        CatalogStatus status) {
    public TaskTypeAttributes attributes() {
        return new TaskTypeAttributes(
                group,
                professional,
                resultType,
                defaultRole,
                defaultImportance,
                windowBeforeMinutes,
                windowAfterMinutes,
                billable,
                serviceId,
                upperLimit,
                vitalSignType,
                certificates,
                null);
    }
}
