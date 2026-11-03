package com.eldercare.backend.health.api;

import com.eldercare.backend.health.api.dto.LatestVitalSignView;
import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Đọc chỉ số theo hợp đồng 007 mục 1.2; không ghi hoặc tự sinh số đo. */
public interface VitalSignApi {
    /** Mỗi loại chỉ số tối đa một bản gần nhất của người cao tuổi, dùng giá trị hiện hành sau đính chính. */
    Map<Long, LatestVitalSignView> latestPerType(long residentId);

    /** Bản ghi đã xác nhận trong khoảng [from, to), dùng giá trị hiện hành. */
    List<VitalSignReadingView> withinRange(long residentId, long indicatorId, Instant from, Instant to);

    /** Các mã danh mục chỉ số thuộc nhóm sinh hiệu (FR-051). */
    Set<Long> vitalSignIndicatorIds();

    List<VitalSignHandoverView> aboveThresholdInRange(long floorAreaId, Instant from, Instant to);
}
