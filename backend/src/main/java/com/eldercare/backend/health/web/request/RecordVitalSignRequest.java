package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Thân lệnh ghi chỉ số theo hợp đồng 007. */
public record RecordVitalSignRequest(
        Long indicatorId,
        Long chiSoId,
        Map<String, BigDecimal> values,
        Map<String, BigDecimal> giaTri,
        Instant measuredAt,
        Instant thoiDiemDo,
        Boolean immediateAction,
        Boolean xuLyNgay,
        String notes,
        String ghiChu,
        Long taskId,
        Long congViecId) {

    public RecordVitalSignCommand toCommand(long residentId, long actorId) {
        long effectiveIndicatorId = indicatorId != null ? indicatorId : (chiSoId != null ? chiSoId : 0L);
        Map<String, BigDecimal> effectiveValues = values != null ? values : (giaTri != null ? giaTri : Map.of());
        Instant effectiveMeasuredAt = measuredAt != null ? measuredAt : thoiDiemDo;
        boolean effectiveImmediateAction = Boolean.TRUE.equals(immediateAction) || Boolean.TRUE.equals(xuLyNgay);
        String effectiveNotes = notes != null ? notes : ghiChu;
        Long effectiveTaskId = taskId != null ? taskId : congViecId;

        return new RecordVitalSignCommand(
                residentId,
                effectiveIndicatorId,
                effectiveMeasuredAt,
                actorId,
                effectiveValues,
                effectiveImmediateAction,
                effectiveTaskId,
                null,
                false,
                effectiveNotes);
    }
}
