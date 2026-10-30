package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004/C-13: sự kiện thông báo theo hợp đồng 005 mục 1.11. */
public record TaskSourceChanged(SourceType type, long sourceId, long residentId, CareEventContext context)
        implements CareNotice {
    @Override
    public String keyNamespace() {
        return context.sourceType() + "\u0000" + type;
    }

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }
}
