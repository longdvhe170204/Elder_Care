package com.cambae.backend.m15.account.api.spi;

import com.cambae.backend.common.Reason;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SPI: nhiệm vụ cho quyền theo nhiệm vụ và phạm vi (FR-019a → f, FR-031; 002 research A-06). Khai báo ở 002; M09 (008)
 * cài (008 contracts mục 1.2). Trả id nhân viên.
 */
public interface DutyApi {

    /** Rỗng khi ca thiếu người phụ trách. */
    Optional<Long> shiftLead(long floorAreaId, Instant at);

    /** Giao trưởng tầng đang hiệu lực, kể cả giao tạm. */
    Optional<Long> assignedFloorManager(long floorAreaId, Instant at);

    /** Người phụ trách ca đang diễn ra khi tầng chưa có Trưởng tầng (Q-90). */
    Optional<Long> floorManagerSubstitute(long floorAreaId, Instant at);

    /** FR-019e (014 FR-066, Q-262). */
    boolean shiftHasNoFloorManager(long shiftId);

    Optional<Long> departmentLead(long departmentId);

    boolean isDepartmentLead(long staffId);

    /** DBR-36. */
    Optional<Long> departmentOf(long staffId, Instant at);

    Optional<Long> groupLeader(long tripId);

    List<Long> companions(long tripId);

    /** Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc có phân công trong tầng và ca (011 UC-76). */
    List<Long> floorReceivers(long floorAreaId, long shiftId);

    /**
     * Thay Phụ trách bộ phận trong transaction của lệnh khóa tài khoản người đang giữ nhiệm vụ này (002 T031; Q-292,
     * 008 FR-001a); 008 cài với {@code Propagation.MANDATORY}, kiểm người thay theo luật của 008 (Q-293, Q-299).
     */
    void replaceDepartmentLead(long currentLeadStaffId, long replacementStaffId, Reason reason);
}
