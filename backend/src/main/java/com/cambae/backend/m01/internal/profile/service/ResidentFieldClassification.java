package com.cambae.backend.m01.internal.profile.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường của hồ sơ người cao tuổi theo nhóm thông tin (001 T011; data-model mục 10; FR-049, BR-M15-11): giấy tờ
 * và ảnh là nhận dạng, địa chỉ và liên hệ là liên hệ, mức chăm sóc hiện hành là mức chăm sóc, còn lại công khai nội bộ.
 * Nhờ đó Hành chính xem lịch sử hồ sơ thấy trường nhạy cảm "đã thay đổi" mà không thấy giá trị.
 */
@Component
class ResidentFieldClassification implements FieldClassificationProvider {

    static final String ENTITY_TYPE = "M01.RESIDENT";

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of(
                "idDocumentType", InfoGroup.IDENTITY,
                "idDocumentNumber", InfoGroup.IDENTITY,
                "photoFileId", InfoGroup.IDENTITY,
                "permanentAddress", InfoGroup.CONTACT,
                "contactAddress", InfoGroup.CONTACT,
                "contact", InfoGroup.CONTACT,
                "currentCareLevel", InfoGroup.CARE_LEVEL,
                // văn bản tự do, có thể chứa tình trạng sức khỏe
                "specialNotes", InfoGroup.HEALTH);
    }
}
