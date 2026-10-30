package com.eldercare.backend.administration.account.api.spi;

import java.time.Instant;
import java.util.List;

/**
 * SPI: phân công của nhân viên cho phạm vi dữ liệu (FR-028 → FR-035a). Khai báo ở 002; M09 (008) cài (008 contracts
 * mục 1.1, FR-037a).
 */
public interface AssignmentApi {

    /** Phân công đang hiệu lực tại thời điểm đã cho. */
    List<AssignmentView> activeAt(long staffId, Instant at);

    /** Phân công có hiệu lực giao với khoảng đã cho (FR-035a). */
    List<AssignmentView> history(long staffId, Instant from, Instant to);

    /**
     * Nhân viên đang có phân công cho người cao tuổi tại thời điểm {@code at}, trực tiếp hoặc qua phòng, tầng người đó
     * đang ở theo luật phân công của 008; trả id nhân viên. 009 dùng cho nhóm Điều dưỡng phụ trách, nhân viên phân công
     * (009 FR-007 (a), BR-M13-04; "Đổi api" 2026-10-22).
     */
    List<Long> staffAssignedToResident(long residentId, Instant at);

    /** Nhân viên đang có phân công cho phòng tại thời điểm {@code at}; trả id nhân viên (009 FR-007 (a)). */
    List<Long> staffAssignedToRoom(long roomId, Instant at);
}
