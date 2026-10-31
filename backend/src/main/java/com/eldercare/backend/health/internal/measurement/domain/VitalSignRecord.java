package com.eldercare.backend.health.internal.measurement.domain;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** Bản đọc bất biến; dòng chờ đo lại được khóa đúng một lần khi xác nhận (R-05). */
@DataGroup(DataGroup.Group.MULTIPLE)
public record VitalSignRecord(
        long id,
        long residentId,
        long indicatorId,
        Instant measuredAt,
        VitalSignLevel level,
        VitalSignConfirmationStatus confirmationStatus,
        Map<String, VitalSignComponent> components,
        Set<VitalSignLabel> labels,
        Instant lockedAt) {
    public VitalSignRecord {
        components = Map.copyOf(components);
        labels = Set.copyOf(labels);
    }
}
