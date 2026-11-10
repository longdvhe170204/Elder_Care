package com.eldercare.backend.roombedallocation.api;

import java.time.Instant;
import java.util.Set;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 003 T053, T054 (FR-024a (c), FR-045; research G-08; chủ dự án duyệt 2026-11-09): lệnh 007 gọi trong transaction của
 * lệnh gỡ khoanh vùng, thêm người vào danh sách tiếp xúc. Chiều incident → roombedallocation như
 * {@code IncidentAllocationStatus}; 003 không nghe sự kiện của 007. Lỗi thì ném để lệnh 007 rollback.
 */
public interface InfectionControlCommandApi {
    /**
     * Sau khi gỡ vùng khoanh ({@code floorIds}, {@code areaIds} của vùng vừa gỡ): gỡ dấu "chờ bác sĩ xác nhận vùng cách
     * ly" của phân bổ Hiệu lực ở các phòng thuộc vùng đó mà tại {@code at} không còn chịu chặn (không cách ly phòng,
     * không thuộc vùng khoanh khác; đọc lại {@code AllocationInfectionStatus.cordonedAt}). Gọi lặp không tác động thêm.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void cordonLifted(Set<Long> floorIds, Set<Long> areaIds, Instant at);

    /**
     * Sau khi thêm {@code residentId} vào danh sách tiếp xúc: nếu người đó vừa rời một giường còn Chờ sẵn sàng thì báo
     * Trưởng tầng của tầng (FR-045, sự kiện {@code FormerOccupantListed}); giường đã về Trống thì không làm gì. Khóa
     * thông báo theo giường và lần rời giường nên gọi lặp không tạo thông báo mới.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void contactListed(long residentId, Instant at);
}
