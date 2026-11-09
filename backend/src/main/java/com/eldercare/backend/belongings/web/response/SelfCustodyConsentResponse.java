package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.internal.consent.domain.SelfCustodyConsent;

/** Schema {@code SelfCustodyConsentSummary} của api-013: bản đồng ý cho tự giữ đang Hiệu lực của đồ (FR-015a). */
public record SelfCustodyConsentResponse(long id, String status, String method, long representativeId) {

    public static SelfCustodyConsentResponse of(SimpleRecord<SelfCustodyConsent> consent) {
        return consent == null
                ? null
                : new SelfCustodyConsentResponse(
                        consent.id(),
                        consent.status(),
                        consent.content().method(),
                        consent.content().representativeId());
    }
}
