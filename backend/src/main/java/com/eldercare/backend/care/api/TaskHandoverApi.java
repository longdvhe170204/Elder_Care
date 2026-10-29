package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.TaskHandoverView;
import java.util.*;

/** 005 mục 1.5; chủ phần C cài và giữ bản giả dùng chung. */
public interface TaskHandoverApi {
    List<TaskHandoverView> unclosed(long floorAreaId, long shiftId);

    Map<Long, TaskHandoverView> currentStatus(Collection<Long> taskIds);
    /** MANDATORY: ghi lịch sử cùng lệnh thêm mục của 008. */
    void recordedInHandover(long handoverId, Collection<Long> taskIds);
}
