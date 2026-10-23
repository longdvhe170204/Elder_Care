package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Kết quả ghi nhận mục sức khỏe (contracts/api-001.yaml, schema {@code HealthItemResult}; tên trường {@code items} theo
 * hợp đồng dù chứa một mục).
 */
@Schema(name = "HealthItemResult")
public record HealthItemResultBody(HealthItemBody items, List<Warning> warnings) {

    public static HealthItemResultBody of(RecordResult result) {
        return new HealthItemResultBody(HealthItemBody.of(result.item()), result.warnings());
    }
}
