package com.cambae.backend.m06.api;

import com.cambae.backend.m06.api.dto.VitalSignHandoverView;
import java.time.Instant;
import java.util.List;

/** Phần bàn giao của 007 mục 1.2; các hàm khác do D bổ sung theo task nguồn. */
public interface VitalSignApi {
    List<VitalSignHandoverView> aboveThresholdInRange(long floorAreaId, Instant from, Instant to);
}
