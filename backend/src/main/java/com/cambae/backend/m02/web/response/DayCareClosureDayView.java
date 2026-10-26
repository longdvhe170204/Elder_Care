package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.absence.domain.ClosureDay;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Ngày khu bán trú nghỉ (contracts/api-004.yaml, schema {@code DayCareClosureDay}; FR-060a). */
@Schema(name = "DayCareClosureDay")
public record DayCareClosureDayView(long id, LocalDate date, String reason, boolean lateDeclaration, Status status) {

    public enum Status {
        ACTIVE,
        CANCELLED
    }

    public static DayCareClosureDayView of(ClosureDay day) {
        return new DayCareClosureDayView(
                day.id(),
                day.date(),
                day.reason(),
                day.lateDeclaration(),
                day.active() ? Status.ACTIVE : Status.CANCELLED);
    }
}
