package com.eldercare.backend.incident.internal.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tọa độ mới nhất của cảnh báo cho nhân viên có quyền xem cảnh báo (api-007 {@code LocationView}; FR-047h): "có thể đã
 * cũ" khi quá hai chu kỳ CFG-M05-09; vị trí chính khi người cao tuổi đang Hoạt động bên ngoài, ngược lại là tham khảo.
 */
public record AlertLocationView(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        Instant measuredAt,
        boolean possiblyStale,
        boolean primary) {}
