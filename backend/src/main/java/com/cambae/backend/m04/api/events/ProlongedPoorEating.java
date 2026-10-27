package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record ProlongedPoorEating(long residentId, List<Long> mealIds, Instant at, CareEventContext context)
        implements CareEvent {
    public ProlongedPoorEating {
        mealIds = List.copyOf(mealIds);
    }
}
