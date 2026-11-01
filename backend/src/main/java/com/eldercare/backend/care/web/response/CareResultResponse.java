package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.result.domain.CareResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

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
        return new CareResultResponse(
                result.id(),
                result.id(),
                health ? result.resultType() : null,
                health ? result.values() : Map.of(),
                false,
                false,
                result.performedAt(),
                result.recordedAt(),
                result.deviceAt(),
                result.lateRecording(),
                performer,
                result.permissionBasis(),
                health ? result.vitalSignRecordId() : null);
    }
}
