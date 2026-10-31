package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.common.data.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** Chỉ sáu trường ResultValue đã công bố. */
@Schema(name = "ResultValue")
public record ResultValueResponse(
        long id, String type, String code, String name, String ruleGroup, CatalogStatus status) {
    public static ResultValueResponse of(CatalogItem<ResultValueAttributes> row) {
        var a = row.attributes();
        return new ResultValueResponse(
                CareCatalogKinds.publicId(row), a.type(), a.code(), row.name(), a.ruleGroup(), row.status());
    }
}
