package com.eldercare.backend.belongings;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * 013 "Đổi api" (chủ dự án duyệt 2026-11-07; FR-015a, FR-017b; plan điểm 27): chi tiết đồ và đồ trên cổng người thân có
 * {@code selfCustodyConsent} (bản đồng ý Hiệu lực) để Hành chính, người đại diện rút đồng ý đồ đã có đồng ý từ trước.
 */
class FR_015a_SelfCustodyConsentReadIT extends ReturnTestBase {

    @Test
    void FR_015a_detailAndPortalShowEffectiveConsent_untilWithdrawn() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long representative = representative(resident, "012345");
        long item = intake(resident, "JEWELRY", "\"quantity\": 1", location());
        String detail = "/api/v1/belongings/" + item;
        String portal = "/api/v1/family-portal/residents/" + resident + "/belongings";

        getJson(tokenA, detail)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selfCustodyConsent").doesNotExist());

        long consent = id(postJson(
                        tokenA,
                        "/api/v1/self-custody-consents",
                        "{\"belongingId\": " + item + ", \"representativeId\": " + representative
                                + ", \"signedFormFileId\": " + photo() + "}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        getJson(tokenA, detail)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selfCustodyConsent.id").value(consent))
                .andExpect(jsonPath("$.selfCustodyConsent.status").value("EFFECTIVE"))
                .andExpect(jsonPath("$.selfCustodyConsent.method").value("SIGNED_FORM"))
                .andExpect(jsonPath("$.selfCustodyConsent.representativeId").value(representative));
        getJson(portalSession(representative), portal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(item))
                .andExpect(jsonPath("$[0].selfCustodyConsent.id").value(consent))
                .andExpect(jsonPath("$[0].selfCustodyConsent.representativeId").value(representative));

        postJson(
                        tokenA,
                        "/api/v1/self-custody-consents/" + consent + "/withdraw",
                        "{\"reason\": \"Người đại diện rút đồng ý\", \"representativeId\": " + representative
                                + ", \"signedFormFileId\": " + photo() + "}")
                .andExpect(status().isOk());

        getJson(tokenA, detail).andExpect(jsonPath("$.selfCustodyConsent").doesNotExist());
        getJson(portalSession(representative), portal)
                .andExpect(jsonPath("$[0].selfCustodyConsent").doesNotExist());
    }
}
