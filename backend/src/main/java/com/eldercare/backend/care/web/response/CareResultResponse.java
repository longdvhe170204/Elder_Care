package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.care.internal.result.domain.CareResult;
import com.eldercare.backend.common.error.BusinessException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

@Schema(name = "CareTaskResult")
public record CareResultResponse(
        long id,
        long taskId,
        String resultType,
        Map<String, Object> currentValue,
        boolean corrected,
        boolean recordingCancelled,
        Instant performedAt,
        Instant recordedAt,
        Instant deviceAt,
        boolean lateRecording,
        String performer,
        String permissionBasis,
        Long vitalSignRecordId) {
    public static CareResultResponse of(CareResult result, String performer, boolean health) {
        return of(result, performer, health, null);
    }

    public static CareResultResponse of(CareResult result, String performer, boolean health, CurrentValue current) {
        boolean corrected = current != null && current.corrected();
        boolean voided = current != null && current.voided();
        Map<String, Object> values = result.values();
        if (corrected && current.content() != null) {
            if (!(current.content() instanceof Map<?, ?> map)) throw unavailable();
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw unavailable();
                copy.put(key, entry.getValue());
            }
            values = Collections.unmodifiableMap(copy);
        } else if (corrected && !voided) throw unavailable();
        return new CareResultResponse(
                result.id(),
                result.id(),
                health ? result.resultType() : null,
                health ? values : Map.of(),
                corrected,
                voided,
                result.performedAt(),
                result.recordedAt(),
                result.deviceAt(),
                result.lateRecording(),
                performer,
                result.permissionBasis(),
                health ? result.vitalSignRecordId() : null);
    }

    private static BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Chưa đọc được nội dung đính chính kết quả");
    }
}
