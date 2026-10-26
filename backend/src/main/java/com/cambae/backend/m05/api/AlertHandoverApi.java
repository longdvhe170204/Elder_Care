package com.cambae.backend.m05.api;

import com.cambae.backend.m05.api.dto.AlertHandoverView;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 007 mục 1.5: đọc nguồn và chuyển trách nhiệm cùng transaction của 008. */
public interface AlertHandoverApi {
    List<AlertHandoverView> open(long floorAreaId, long shiftId);

    record SourceRef(String group, long id) {}

    Map<SourceRef, AlertHandoverView> currentStatus(Collection<SourceRef> sources);

    void onHandoverConfirmed(long handoverId, long previousShiftId, Long nextShiftId, long floorAreaId);
}
