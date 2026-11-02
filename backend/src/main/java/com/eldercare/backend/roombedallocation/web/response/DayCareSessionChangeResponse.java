package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.internal.daycare.domain.DayCareSessionChange;
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
