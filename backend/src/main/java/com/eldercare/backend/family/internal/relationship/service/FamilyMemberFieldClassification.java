package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của hồ sơ người thân ({@code PhanLoaiTruongProvider}; 012 T008, research F-17, FR-068): giấy
 * tờ tùy thân là dữ liệu cá nhân, vai trò không cần thấy "đã thay đổi" mà không thấy giá trị.
 */
@Component
class FamilyMemberFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return FamilyMember.ENTITY_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of("idDocumentType", InfoGroup.IDENTITY, "idDocumentNumber", InfoGroup.IDENTITY);
    }
}
