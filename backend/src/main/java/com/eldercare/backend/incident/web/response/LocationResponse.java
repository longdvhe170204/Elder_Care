package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.AlertLocationView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** {@code LocationView} của api-007 (FR-047h): tọa độ mới nhất, "có thể đã cũ", vị trí chính hay tham khảo. */
@Schema(name = "LocationView")
public record LocationResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        Instant measuredAt,
        Boolean possiblyStale,
        Boolean isPrimary) {

    public static LocationResponse of(AlertLocationView view) {
        return view == null
                ? null
                : new LocationResponse(
                        view.latitude(),
                        view.longitude(),
                        view.accuracyMeters(),
                        view.measuredAt(),
                        view.possiblyStale(),
                        view.primary());
    }
}
