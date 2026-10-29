package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Tra cứu hồ sơ người cao tuổi cho module khác (contracts mục 1.1); chỉ đọc. */
public interface ResidentApi {

    /** 002 (FR-044a, FR-046, FR-049) và mọi module. */
    ResidentStatus status(long residentId);

    /** Họ tên, ảnh, mã hồ sơ; 007 dùng cho người ngoài phạm vi (002 FR-044a). */
    IdentityView identity(long residentId);

    /** Thời điểm sang trạng thái cuối: 002 khóa tài khoản người thân (CFG-M01-04), lưu giữ. */
    Optional<Instant> finalStatusAt(long residentId);

    boolean isFinalStatus(long residentId);

    /** 003 (BR-M03-01). */
    Optional<Gender> gender(long residentId);

    /** Cho job của module khác. */
    List<Long> byStatus(Set<ResidentStatus> statuses);
}
