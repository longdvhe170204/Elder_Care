package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.internal.daycare.domain.DayCareSessionChange;
import java.util.List;

/** Cảnh báo sau đổi khung, không hủy lịch. */
public record DayCareSessionChangeResponse(
        DayCareSessionResponse session, List<DayCareSessionCapacity> overCapacitySessions) {
    public static DayCareSessionChangeResponse of(DayCareSessionChange value) {
        return new DayCareSessionChangeResponse(
                DayCareSessionResponse.of(value.session()),
                value.overCapacitySessions().stream()
                        .map(DayCareSessionCapacity::of)
                        .toList());
    }
}
