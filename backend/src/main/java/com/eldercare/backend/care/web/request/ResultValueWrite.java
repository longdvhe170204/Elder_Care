package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.catalog.domain.ResultValueAttributes;
import io.swagger.v3.oas.annotations.media.Schema;

/** Chỉ dữ liệu sáu trường ResultValue đã duyệt. */
@Schema(name = "ResultValue")
public record ResultValueWrite(
        Long id,

        @Schema(allowableValues = {"SLEEP_QUALITY", "MOOD", "BEHAVIOR"})
        String type,

        String code,
        String name,
        @Schema(allowableValues = {"INSOMNIA", "NEGATIVE"}) String ruleGroup,
        String status) {
    public ResultValueAttributes attributes() {
        return new ResultValueAttributes(type, code, ruleGroup, null);
    }
}
