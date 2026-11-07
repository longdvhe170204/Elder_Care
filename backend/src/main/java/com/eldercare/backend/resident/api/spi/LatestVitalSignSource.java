package com.eldercare.backend.resident.api.spi;

import com.eldercare.backend.resident.api.dto.LatestVitalSign;
import java.util.List;

/**
 * SPI: health (007) cài, chuyển tiếp sang {@code VitalSignApi.latestPerType} (001 FR-024; "Đổi api" chủ dự án duyệt
 * 2026-11-05). Khai báo ở M01 để không có vòng phụ thuộc resident ↔ health. Chưa có bean cài thì chi tiết đánh giá trả danh
 * sách rỗng.
 */
public interface LatestVitalSignSource {

    /** Mỗi loại chỉ số tối đa một bản gần nhất; không có thì rỗng. */
    List<LatestVitalSign> latest(long residentId);
}
