package com.eldercare.backend.resident.internal.profile.service;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Kiểm nguồn ý kiến dùng chung (001 T012a; FR-023g, BR-M01-14, research H-16): {@code FAMILY_MEMBER} thì người thân phải có
 * quan hệ Hiệu lực với người cao tuổi lúc gọi (qua {@link FamilyRelationshipApi}, M10 cài); loại khác thì
 * {@code familyMemberId} phải rỗng. Sai thì 422 {@code INVALID_OPINION_SOURCE}.
 */
@Service
public class OpinionSourceService implements OpinionSourceApi {

    // ObjectProvider: 012 (M10) cài FamilyRelationshipApi; trước đó chỉ test khai bản giả, và thiếu bản cài thì
    // không người thân nào có quan hệ Hiệu lực
    private final ObjectProvider<FamilyRelationshipApi> relationships;

    OpinionSourceService(ObjectProvider<FamilyRelationshipApi> relationships) {
        this.relationships = relationships;
    }

    @Override
    public void validate(long residentId, OpinionSource source) {
        if (source == null || source.type() == null) {
            throw invalid("Thiếu nguồn ý kiến");
        }
        if (source.type() != OpinionSourceType.FAMILY_MEMBER) {
            if (source.familyMemberId() != null) {
                throw invalid("familyMemberId chỉ dùng khi nguồn là người thân");
            }
            return;
        }
        if (source.familyMemberId() == null) {
            throw invalid("Nguồn người thân cần familyMemberId");
        }
        FamilyRelationshipApi api = relationships.getIfAvailable();
        if (api == null || !api.belongsToResident(source.familyMemberId(), residentId)) {
            throw invalid("Người thân không có quan hệ Hiệu lực với người cao tuổi");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_OPINION_SOURCE", message);
    }
}
