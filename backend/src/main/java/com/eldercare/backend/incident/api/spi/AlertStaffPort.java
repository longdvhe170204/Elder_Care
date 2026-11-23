package com.eldercare.backend.incident.api.spi;

import com.eldercare.backend.incident.api.dto.AlertShiftView;
import java.time.Instant;
import java.util.*;

/** NhanSuCanhBaoPort: phần ca của T015; phân công, truy vết và bàn giao nối cùng story nguồn. */
public interface AlertStaffPort {
    java.util.List<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView> startedWithoutConfirmedHandover(
            Instant from, Instant to);

    Optional<Long> assignedNurse(long residentId, Instant at);

    List<com.eldercare.backend.incident.api.dto.AssignmentTraceView> assignmentsInRange(
            long residentId, Instant from, Instant to);

    Optional<AlertShiftView> shiftAt(long floorAreaId, Instant at);

    Optional<AlertShiftView> nextShift(long floorAreaId, Instant at);

    List<Long> onDutyDoctors(Instant at);

    Optional<Long> assignedFloorManager(long floorAreaId, Instant at);

    boolean listedInShift(long staffId, long shiftId);

    boolean inProgressShiftMember(long staffId, Instant at);

    boolean onShift(long staffId, Instant at);

    Optional<Long> floorDuringShift(long staffId, Instant at);

    List<Long> floorsWithShift(Instant at);

    /**
     * Nhân viên đang làm việc, đang có mặt trong ca của tầng/khu vực lúc {@code at}, để chọn người bàn giao cho lệnh Chuyển
     * viện (FR-049; Đổi api 2026-11-22, chờ chủ dự án duyệt). Mặc định rỗng để bản giả cũ không phải đổi.
     */
    default List<com.eldercare.backend.incident.api.dto.HandoverCandidateView> handoverCandidates(
            long floorAreaId, Instant at) {
        return List.of();
    }
}
