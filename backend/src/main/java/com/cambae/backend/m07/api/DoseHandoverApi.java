package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.DoseHandoverView;
import java.util.*;

/** 006 mục 1.4: đọc không vật hóa liều và không lập mục bàn giao. */
public interface DoseHandoverApi {
    List<DoseHandoverView> inShift(long floorAreaId, long shiftId);

    Map<String, String> currentStatus(Collection<String> doseKeys);
}
