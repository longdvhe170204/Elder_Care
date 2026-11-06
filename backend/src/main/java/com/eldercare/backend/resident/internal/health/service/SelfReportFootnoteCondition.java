package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Điều kiện chú thích ⁵⁰ của Phụ lục 27 (001 T102; Q-400): Hành chính ghi tự khai về sức khỏe chỉ khi người cao tuổi còn
 * Đang tiếp nhận; sau đó quay về không có quyền ở dòng này. Kiểm ở mỗi yêu cầu.
 */
@Component
class SelfReportFootnoteCondition implements FootnoteCondition {

    private final ProfileOpenGuard guard;

    SelfReportFootnoteCondition(ProfileOpenGuard guard) {
        this.guard = guard;
    }

    @Override
    public String footnoteCode() {
        return "50";
    }

    @Override
    public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
        return target.residentId() != null && guard.isAdmitting(target.residentId());
    }
}
