package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.common.data.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** Version chỉ đọc theo hợp đồng được chủ dự án duyệt ngày 31/10. */
@Schema(name = "ResultValue")
public record ResultValueResponse(
        long id,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) int version,
        String type,
        String code,
        String name,
        String ruleGroup,
        CatalogStatus status) {
    public static ResultValueResponse of(CatalogItem<ResultValueAttributes> row) {
        var a = row.attributes();
        return new ResultValueResponse(
                CareCatalogKinds.publicId(row),
                row.version(),
                a.type(),
                a.code(),
                row.name(),
                a.ruleGroup(),
                row.status());
    }
}
