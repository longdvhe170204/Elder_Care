package com.eldercare.backend.health.api;

import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import java.time.Instant;
import java.util.List;

/** Phần bàn giao của 007 mục 1.2; các hàm khác do D bổ sung theo task nguồn. */
public interface VitalSignApi {
    List<VitalSignHandoverView> aboveThresholdInRange(long floorAreaId, Instant from, Instant to);
}
