package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Thân lệnh đo lại chỉ số theo hợp đồng 007. */
public record RemeasureVitalSignRequest(
        Map<String, BigDecimal> values,
        Map<String, BigDecimal> giaTri,
        Instant measuredAt,
        Instant thoiDiemDo,
        String notes,
        String ghiChu) {

    public RemeasureVitalSignCommand toCommand(long draftId, long actorId) {
        Map<String, BigDecimal> effectiveValues = values != null ? values : (giaTri != null ? giaTri : Map.of());
        Instant effectiveMeasuredAt = measuredAt != null ? measuredAt : thoiDiemDo;
        String effectiveNotes = notes != null ? notes : ghiChu;

        return new RemeasureVitalSignCommand(draftId, effectiveMeasuredAt, actorId, effectiveValues, effectiveNotes);
    }
}
