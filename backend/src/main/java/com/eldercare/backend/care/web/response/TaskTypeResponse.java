package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/** Version chỉ đọc để client gửi If-Match khi sửa danh mục. */
@Schema(name = "TaskType")
public record TaskTypeResponse(
        long id,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) int version,
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
    public static TaskTypeResponse of(CatalogItem<TaskTypeAttributes> row) {
        var a = row.attributes();
        return new TaskTypeResponse(
                CareCatalogKinds.publicId(row),
                row.version(),
                row.name(),
                a.group(),
                a.professional(),
                a.resultType(),
                a.defaultRole(),
                a.defaultImportance(),
                a.windowBeforeMinutes(),
                a.windowAfterMinutes(),
                a.billable(),
                a.serviceId(),
                a.upperLimit(),
                a.vitalSignType(),
                a.certificates(),
                row.status());
    }
}
