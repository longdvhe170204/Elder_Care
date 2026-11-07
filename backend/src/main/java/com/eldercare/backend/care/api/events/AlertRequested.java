package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record AlertRequested(
        String type,
        NotificationLevel level,
        Long residentId,
        String source,
        SourceType basisType,
        long basisId,
        CareEventContext context,
        Optional<CareAlertSource> sourceDetails)
        implements CareEvent {
    public AlertRequested {
        sourceDetails = sourceDetails == null ? Optional.empty() : sourceDetails;
        if (residentId == null
                && (sourceDetails.isEmpty() || sourceDetails.get().locationId() == null))
            throw new IllegalArgumentException("Cảnh báo cần người cao tuổi hoặc vị trí nguồn");
    }

    /** Giữ caller và payload cũ của các nguồn cảnh báo gắn người cao tuổi. */
    public AlertRequested(
            String type,
            NotificationLevel level,
            long residentId,
            String source,
            SourceType basisType,
            long basisId,
            CareEventContext context) {
        this(type, level, residentId, source, basisType, basisId, context, Optional.empty());
    }
}
