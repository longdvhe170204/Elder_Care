package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.Warning;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Kết quả ghi nhận mục sức khỏe (contracts/api-001.yaml, schema {@code HealthItemResult}; trường {@code item}, A duyệt đổi
 * từ {@code items} ngày 2026-11-05, glossary ghi {@code item}).
 */
@Schema(name = "HealthItemResult")
public record HealthItemResultBody(HealthItemBody item, List<Warning> warnings) {

    public static HealthItemResultBody of(RecordResult result) {
        return new HealthItemResultBody(HealthItemBody.of(result.item()), result.warnings());
    }
}
