package com.eldercare.backend.billing.balance.internal.document.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền xem chi phí đang bật của người thân (FR-038, Q-378): ma trận quyền chỉ kiểm quan hệ, còn quyền con "xem chi phí"
 * do 002 giữ nên kiểm thêm ở đây, như cổng số dư. Tài khoản nhân viên không bị kiểm.
 */
@Component
public class FamilyCostsRight {

    private final ObjectProvider<CurrentUser> user;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final BusinessTime time;

    FamilyCostsRight(
            ObjectProvider<CurrentUser> user, ObjectProvider<FamilyRelationshipApi> relationships, BusinessTime time) {
        this.user = user;
        this.relationships = relationships;
        this.time = time;
    }

    public boolean allowed(long residentId) {
        CurrentUser current = user.getObject();
        if (current.type() != AccountType.FAMILY_MEMBER) {
            return true;
        }
        FamilyRelationshipApi api = relationships.getIfAvailable();
        return api != null
                && current.familyMemberId()
                        .filter(id -> api.rightEnabled(id, residentId, FamilyRight.VIEW_COSTS, time.now()))
                        .isPresent();
    }

    public void require(long residentId) {
        if (!allowed(residentId)) {
            throw new PermissionDeniedException("Người thân chưa có quyền xem chi phí của người cao tuổi này");
        }
    }
}
