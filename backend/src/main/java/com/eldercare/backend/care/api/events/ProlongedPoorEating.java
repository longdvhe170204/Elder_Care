package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record ProlongedPoorEating(long residentId, List<Long> mealIds, Instant at, CareEventContext context)
        implements CareEvent {
    public ProlongedPoorEating {
        mealIds = List.copyOf(mealIds);
    }
}
