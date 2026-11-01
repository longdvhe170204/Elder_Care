package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.task.domain.CareTaskDraft;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

@Schema(name = "CareTaskWrite")
public record CareTaskWrite(
        Long residentId,
        Location location,
        Long taskTypeId,
        Basis basis,
        String content,
        Instant plannedAt,
        Integer windowBeforeMinutes,
        Integer windowAfterMinutes,
        Importance importance,
        Long performerId,
        Boolean shared) {
    public record Location(String type, Long id) {}

    public record Basis(SourceType type, Long id) {}

    public CareTaskDraft draft() {
        if (taskTypeId == null
                || taskTypeId <= 0
                || basis == null
                || basis.type() == null
                || plannedAt == null
                || importance == null
                || (residentId == null && location == null)
                || (residentId != null && residentId <= 0)
                || (location != null
                        && (location.id() == null
                                || location.id() <= 0
                                || location.type() == null
                                || !Set.of("ROOM", "BED", "FLOOR", "AREA").contains(location.type())))
                || (windowBeforeMinutes != null && (windowBeforeMinutes < 0 || windowBeforeMinutes > 32767))
                || (windowAfterMinutes != null && (windowAfterMinutes < 0 || windowAfterMinutes > 32767))
                || (content != null && content.length() > 1000))
            throw new InvalidValueException("Dòng công việc thiếu hoặc sai giá trị");
        return new CareTaskDraft(
                residentId,
                location == null ? null : location.type(),
                location == null ? null : location.id(),
                taskTypeId,
                basis.type(),
                basis.id(),
                content,
                plannedAt,
                windowBeforeMinutes,
                windowAfterMinutes,
                importance,
                performerId,
                Boolean.TRUE.equals(shared));
    }
}
