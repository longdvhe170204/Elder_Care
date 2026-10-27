package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
import com.cambae.backend.m04.api.spi.TaskSourcePort.SourceType;
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
