package com.eldercare.backend.family.internal.right.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
import com.eldercare.backend.resident.api.ConsentApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Điều kiện bật một quyền, dùng chung cho phiếu đăng ký (FR-008) và yêu cầu thay đổi quyền (FR-013): FR-007 (giấy tờ của
 * người được phép đón), FR-010 (chỉ người đại diện được yêu cầu thay đổi dịch vụ), FR-011 (bản đồng ý gồm người thân khi
 * bật xem sức khỏe); chạy lúc lập và lúc xác nhận, duyệt.
 */
@Component
public class RightGrantRules {

    private final FamilyRelationshipStore store;
    private final ConsentApi consents;

    RightGrantRules(FamilyRelationshipStore store, ConsentApi consents) {
        this.store = store;
        this.consents = consents;
    }

    public void check(long residentId, FamilyRelationship relationship, FamilyRight right) {
        switch (right) {
            case PICKUP_ALLOWED -> {
                FamilyMember member =
                        store.familyMember(relationship.familyMemberId()).orElseThrow();
                if (member.idDocumentType() == null || member.idDocumentNumber() == null) {
                    throw idDocumentRequired();
                }
            }
            case REQUEST_SERVICES -> {
                if (!relationship.representative()) {
                    throw requestNotAllowed(
                            HttpStatus.CONFLICT, "Quyền được yêu cầu thay đổi dịch vụ chỉ cho người đại diện (FR-010)");
                }
            }
            case VIEW_HEALTH -> {
                if (!consents.includes(residentId, relationship.familyMemberId())) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "CONSENT_NOT_INCLUDED",
                            "Bản đồng ý Hiệu lực của người cao tuổi không bao gồm người thân này (FR-011)");
                }
            }
            default -> {}
        }
    }

    public static BusinessException idDocumentRequired() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "ID_DOCUMENT_REQUIRED",
                "Người được phép đón phải có loại và số giấy tờ tùy thân (FR-007)");
    }

    public static BusinessException requestNotAllowed(HttpStatus status, String message) {
        return new BusinessException(status, "REQUEST_NOT_ALLOWED", message);
    }
}
