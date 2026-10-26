package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.FootnoteCondition;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Điều kiện chú thích 39 của Phụ lục 27 (Q-222, Q-313; FR-064): Kế toán chỉ xem hồ sơ kết thúc lưu trú và lập yêu cầu ngoại lệ
 * cho điều kiện quyết toán số dư, tiền cọc. Thỏa khi đối tượng là hồ sơ kết thúc (xem) hoặc yêu cầu ngoại lệ ý tiền; lệnh Kết
 * thúc lưu trú, ghi nhận qua đời của 001 mang đối tượng người cao tuổi nên không thỏa. Các lệnh khác trên hồ sơ kết thúc
 * ({@link ResidencyEndingService}) vẫn chặn Kế toán theo vai trò.
 */
@Component
class EndingFootnoteCondition implements FootnoteCondition {

    @Override
    public String footnoteCode() {
        return "39";
    }

    @Override
    public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
        return ResidencyEndingService.ENTITY_TYPE.equals(target.type())
                || EndingExceptionHandler.SETTLEMENT_ENTITY.equals(target.type());
    }
}
