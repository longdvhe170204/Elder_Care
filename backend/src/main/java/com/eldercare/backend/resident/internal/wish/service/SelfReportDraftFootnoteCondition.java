package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.status.persistence.ResidentStatusStore;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Điều kiện chú thích ⁵² của Phụ lục 27 (001 T110; Q-412, BR-M01-18): Hành chính lập bản Nháp tự khai của phiếu đánh giá
 * đầu vào, phiếu nguyện vọng chỉ khi người cao tuổi còn Đang tiếp nhận; sau đó quay về không có quyền ở các dòng này.
 */
@Component
class SelfReportDraftFootnoteCondition implements FootnoteCondition {

    private final ResidentStatusStore statuses;

    SelfReportDraftFootnoteCondition(ResidentStatusStore statuses) {
        this.statuses = statuses;
    }

    @Override
    public String footnoteCode() {
        return "52";
    }

    @Override
    public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
        // không có hồ sơ thì không thỏa, không ném lỗi (lớp vai trò chỉ trả cho phép / từ chối)
        return target.residentId() != null
                && statuses.find(target.residentId())
                        .filter(s -> s == ResidentStatus.ADMISSION_IN_PROGRESS)
                        .isPresent();
    }
}
