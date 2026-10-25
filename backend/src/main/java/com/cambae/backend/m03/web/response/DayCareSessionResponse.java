package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.internal.daycare.domain.DayCareSessionState;
import java.time.format.DateTimeFormatter;

/** Giờ trên dây luôn HH:mm, không phụ thuộc serializer LocalTime. */
public record DayCareSessionResponse(long id, String name, String startTime, String endTime, String status) {
    public static DayCareSessionResponse of(DayCareSessionState value) {
        return new DayCareSessionResponse(
                value.id(),
                value.name(),
                value.startTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                value.endTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                value.status());
    }
}
