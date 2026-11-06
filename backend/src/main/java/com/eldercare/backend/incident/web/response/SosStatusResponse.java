package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.SosStatusView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Trạng thái SOS cho app người cao tuổi ({@code SosStatus} của api-007; FR-047f). Vị trí GPS thêm ở T096. */
@Schema(name = "SosStatus")
public record SosStatusResponse(
        long pressId,
        long alertId,

        @Schema(allowableValues = {"SENT", "RECEIVED", "ARRIVED"})
        String status,

        String receiverName,
        Instant arrivedAt,
        Boolean sendLocation,
        Integer locationIntervalSeconds,
        FallbackContacts contacts) {

    /** Số liên hệ của viện và số cấp cứu để app hiện khi lỗi hoặc mất kết nối (012 FR-085). */
    @Schema(name = "FallbackContacts")
    public record FallbackContacts(String facilityPhone, String emergencyNumber) {}

    public static SosStatusResponse of(SosStatusView view) {
        return new SosStatusResponse(
                view.pressId(),
                view.alertId(),
                view.status(),
                view.receiverName(),
                view.arrivedAt(),
                null,
                null,
                new FallbackContacts(view.facilityPhone(), "115"));
    }
}
