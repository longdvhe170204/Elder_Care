package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.AllocationHandoverView;
import java.time.Instant;
import java.util.List;

/** Nguồn đọc của 003 mục 1.9; 008 không tự đổi trạng thái phân bổ. */
public interface AllocationHandoverApi {
    List<AllocationHandoverView> atFloor(long floorId, Instant at);
}
