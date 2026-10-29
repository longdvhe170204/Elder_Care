package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.status.persistence.ResidentStatusStore;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Chặn ghi mới trên hồ sơ ở trạng thái cuối: Kết thúc lưu trú, Qua đời, Hủy tiếp nhận (BR-M01-05, FR-044); trả 409
 * {@code PROFILE_CLOSED}. Mọi lệnh nghiệp vụ ghi vào hồ sơ của M01 gọi {@link #requireOpen} ngay trong lệnh sau khi khóa
 * dòng hồ sơ ({@code ProfileOpenGuardArchTest} kiểm). Ngoại lệ có chủ đích: rút lại bản đồng ý (FR-014a), đính chính
 * (FR-049a) và lệnh danh mục không gắn hồ sơ không gọi.
 */
@Component
public class ProfileOpenGuard {

    static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final ResidentStatusStore statuses;

    ProfileOpenGuard(ResidentStatusStore statuses) {
        this.statuses = statuses;
    }

    /** Hồ sơ phải tồn tại và chưa ở trạng thái cuối; gọi trong transaction của lệnh. */
    public void requireOpen(long residentId) {
        ResidentStatus status = statuses.find(residentId).orElseThrow(NotFoundException::new);
        if (FINAL_STATUSES.contains(status)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROFILE_CLOSED",
                    "Hồ sơ đã ở trạng thái cuối, không ghi mới được (BR-M01-05)");
        }
    }
}
