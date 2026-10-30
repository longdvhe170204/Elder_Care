package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004/C-13: sự kiện thông báo theo hợp đồng 005 mục 1.11. */
public record CarePlanApprovalWithdrawn(long planId, long residentId, CareEventContext context) implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }
}
