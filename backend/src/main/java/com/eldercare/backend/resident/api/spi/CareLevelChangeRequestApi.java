package com.eldercare.backend.resident.api.spi;

import java.util.List;

/** SPI: M02 (004) cài; khai báo ở M01 để không có vòng M01 ↔ M02 (004 research T-02). */
public interface CareLevelChangeRequestApi {

    /**
     * {@code Propagation.MANDATORY}: yêu cầu đổi mức đang mở bị thay thế bởi đánh giá đã xác nhận (FR-036, FR-036a);
     * trả id các yêu cầu bị thay thế.
     */
    List<Long> supersedeByAssessment(long residentId, long assessmentId);

    /** Dùng cho job nhắc. */
    boolean hasOpenRequest(long residentId);
}
