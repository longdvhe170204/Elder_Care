package com.eldercare.backend.billing.balance.internal.document.service;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Họ tên người đại diện hiện tại cho phiếu báo phí và bảng quyết toán (017 FR-034, BR-M11-22; chủ dự án duyệt
 * {@code RepresentativePort.activeRepresentative} 2026-11-10). Chưa có người đại diện hoặc nguồn thì trả {@code null},
 * bộ dựng bỏ dòng.
 */
@Component
public class RepresentativeNames {

    private final ObjectProvider<RepresentativePort> representatives;
    private final ObjectProvider<FamilyRelationshipApi> relationships;

    RepresentativeNames(
            ObjectProvider<RepresentativePort> representatives, ObjectProvider<FamilyRelationshipApi> relationships) {
        this.representatives = representatives;
        this.relationships = relationships;
    }

    public String of(long residentId) {
        RepresentativePort port = representatives.getIfAvailable();
        FamilyRelationshipApi api = relationships.getIfAvailable();
        if (port == null || api == null) {
            return null;
        }
        return port.activeRepresentative(residentId)
                .flatMap(api::familyMemberName)
                .orElse(null);
    }
}
