package com.eldercare.backend.care.api.dto;

import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import java.time.Instant;

/** Công việc lập từ một căn cứ, cho module giữ căn cứ hiện lại trong hồ sơ của mình (005 mục 1.6a). */
public record SourceTaskView(
        long id,
        SourceType basisType,
        long sourceId,
        long shiftId,
        String taskTypeName,
        String content,
        Instant plannedAt,
        Instant windowTo,
        Importance importance,
        String performer,
        boolean shared,
        TaskStatus status,
        Instant closedAt,
        String resultNote,
        String closureReason) {}
