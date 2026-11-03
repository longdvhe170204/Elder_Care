package com.eldercare.backend.health.web.response;

import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Phản hồi bản ghi chỉ số theo hợp đồng 007; {@code measuredBy} là họ tên người đo. */
public record VitalSignRecordResponse(
        long id,
        long indicatorId,
        Map<String, BigDecimal> values,
        Map<String, BigDecimal> originalValues,
        Instant measuredAt,
        String measuredBy,
        ThresholdSummary threshold,
        VitalSignLevel level,
        VitalSignConfirmationStatus confirmationStatus,
        Instant remeasurementDeadline,
        Set<VitalSignLabel> labels,
        Long remeasurementFor,
        List<String> hiddenFields) {

    public record ThresholdSummary(
            Long versionId,

            @Schema(allowableValues = {"FACILITY", "PERSONAL"})
            String scope) {}

    public static VitalSignRecordResponse from(VitalSignRecord record, String measuredByName) {
        ThresholdSummary threshold = record.thresholdVersionId() != null
                ? new ThresholdSummary(record.thresholdVersionId(), "FACILITY")
                : null;

        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (record.components() != null) {
            record.components().forEach((k, v) -> values.put(k, v.value()));
        }

        return new VitalSignRecordResponse(
                record.id(),
                record.indicatorId(),
                values,
                null,
                record.measuredAt(),
                measuredByName,
                threshold,
                record.level(),
                record.confirmationStatus(),
                record.remeasurementDeadline(),
                record.labels(),
                record.remeasurementFor(),
                List.of());
    }
}
