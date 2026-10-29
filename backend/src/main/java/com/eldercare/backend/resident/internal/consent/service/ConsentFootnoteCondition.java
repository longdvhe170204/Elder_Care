package com.eldercare.backend.resident.internal.consent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.resident.internal.consent.persistence.ConsentStore;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Điều kiện chú thích ¹ của Phụ lục 27 (001 T056; BR-M01-08, DBR-03; research H-06): người thân chỉ xem được thông tin
 * sức khỏe của người cao tuổi khi bản đồng ý Hiệu lực của người đó gồm họ. Kiểm ở mỗi yêu cầu, không đệm (002 A-07), nên
 * rút lại hoặc thay bản mới có hiệu lực từ lần truy cập kế tiếp (FR-015, SC-008).
 */
@Component
class ConsentFootnoteCondition implements FootnoteCondition {

    private final ConsentStore store;

    ConsentFootnoteCondition(ConsentStore store) {
        this.store = store;
    }

    @Override
    public String footnoteCode() {
        return "1";
    }

    @Override
    public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
        if (target.residentId() == null || user.familyMemberId().isEmpty()) {
            return false;
        }
        return store.activeIncludesAt(target.residentId(), user.familyMemberId().get(), at);
    }
}
