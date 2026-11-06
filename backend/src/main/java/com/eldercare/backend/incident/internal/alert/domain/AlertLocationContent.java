package com.eldercare.backend.incident.internal.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một dòng vị trí của cảnh báo ({@code vi_tri_canh_bao}, data-model mục 7; R-15: {@code record_event} kind
 * {@code M05.ALERT_LOCATION}, cha là cảnh báo; nhóm 3, chỉ thêm). {@code pressType} {@code SOS} hoặc {@code ASSIST}; thời
 * điểm nhận là {@code recordedAt} của dòng.
 */
public record AlertLocationContent(
        String pressType,
        Long pressId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        Instant measuredAt) {}
