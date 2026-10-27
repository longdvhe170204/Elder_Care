package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
import java.time.*;
import java.util.*;

/** T004/C-13: sự kiện thông báo theo hợp đồng 005 mục 1.11. */
public record SharedTaskWithoutQualifiedStaff(long taskId, long shiftId, long floorAreaId, CareEventContext context)
        implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }
}
