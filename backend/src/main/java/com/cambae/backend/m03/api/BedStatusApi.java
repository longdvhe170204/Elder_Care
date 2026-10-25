package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.AwaitingReadyBedView;
import java.time.Instant;
import java.util.List;

/** Phần nguồn bàn giao của 003 mục 1.8, không khai báo thay các hàm thống kê chưa cần. */
public interface BedStatusApi {
    List<AwaitingReadyBedView> awaitingReadinessOverdue(long floorAreaId, Instant at);
    /** Trạng thái thật; thiếu khóa nghĩa là chưa đọc được, không suy ra từ danh sách quá hạn. */
    java.util.Map<Long, String> currentState(java.util.Collection<Long> bedIds, Instant at);
}
