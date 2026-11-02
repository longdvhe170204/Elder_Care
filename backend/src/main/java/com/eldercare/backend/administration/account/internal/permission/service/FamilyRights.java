package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.common.notification.FamilyRight;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền được cấp riêng cho người thân với một người cao tuổi (FR-045 (3), FR-046; 002 T100), đọc qua
 * {@link FamilyRelationshipApi#rightEnabled} của 012. Người dùng không phải người thân, đối tượng không gắn người cao tuổi
 * hoặc chưa có bản cài của 012 thì coi là đang bật (giữ như trước T100).
 */
@Component
class FamilyRights {

    private final ObjectProvider<FamilyRelationshipApi> relationships;

    FamilyRights(ObjectProvider<FamilyRelationshipApi> relationships) {
        this.relationships = relationships;
    }

    boolean enabled(CurrentUser user, Target target, FamilyRight right, Instant at) {
        if (user.familyMemberId().isEmpty() || target.residentId() == null) {
            return true;
        }
        FamilyRelationshipApi api = relationships.getIfAvailable();
        return api == null || api.rightEnabled(user.familyMemberId().get(), target.residentId(), right, at);
    }
}
