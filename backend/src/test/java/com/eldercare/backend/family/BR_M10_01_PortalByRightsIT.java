package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/**
 * 012 T026 ({@code BR_M10_01_CongTheoQuyenIT}; US5 kịch bản 1 → 5, 7; FR-046, FR-047, BR-M10-01): R (người đại diện, xem
 * chi phí), M (xem sức khỏe), N (không quyền riêng) của A; B là người cao tuổi khác. Cổng chỉ có người cao tuổi có quan
 * hệ Hiệu lực, kèm quyền có tác dụng; phần sức khỏe cần quyền đang bật và bản đồng ý bao gồm người thân; hợp đồng chỉ người
 * đại diện; không mở được người cao tuổi khác.
 */
class BR_M10_01_PortalByRightsIT extends FamilyCommandTestBase {

    long a;
    long b;
    long rMember;
    long mMember;
    long nMember;
    long n;
    private final java.util.Map<Long, String> sessions = new java.util.HashMap<>();

    @BeforeEach
    void portalFamily() {
        a = resident(ResidentStatus.IN_RESIDENCE);
        b = resident(ResidentStatus.IN_RESIDENCE);
        rMember = fixtures.familyMember("Trần R", "0905000001");
        mMember = fixtures.familyMember("Lê M", "0905000002");
        nMember = fixtures.familyMember("Phạm N", "0905000003");
        fixtures.relate(rMember, a, 1, true, true, FamilyRight.VIEW_COSTS);
        fixtures.relate(mMember, a, 2, false, false, FamilyRight.VIEW_HEALTH);
        n = fixtures.relate(nMember, a, 3, false, false);
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, free_text, source_id, recorded_by, recorded_at)"
                        + " VALUES (?, 'CHRONIC_CONDITION', 'Tăng huyết áp', (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE'),"
                        + " 1, NOW(6))",
                a);
    }

    private ResultActions portalGet(long familyMemberId, String path) throws Exception {
        return mvc.perform(get("/api/v1/family-portal" + path)
                .header("Authorization", sessions.computeIfAbsent(familyMemberId, this::familySession)));
    }

    @Test
    void US5_list_onlyActiveRelationships_withEffectiveRights() throws Exception {
        consents.active(a, Set.of(mMember), false);
        JsonNode list = json(portalGet(mMember, "/residents").andExpect(status().isOk()));
        assertThat(list).singleElement().satisfies(r -> {
            assertThat(r.get("residentId").asLong()).isEqualTo(a);
            assertThat(r.get("rights").get("VIEW_HEALTH").asBoolean()).isTrue();
            assertThat(r.get("rights").get("VIEW_COSTS").asBoolean()).isFalse();
            assertThat(r.get("representative").asBoolean()).isFalse();
        });
        JsonNode representative = json(portalGet(rMember, "/residents").andExpect(status().isOk()));
        assertThat(representative.get(0).get("representative").asBoolean()).isTrue();
        assertThat(representative.get(0).get("rights").get("VIEW_COSTS").asBoolean())
                .isTrue();
    }

    @Test
    void US5_7_endedRelationship_residentDisappears() throws Exception {
        command(n, "end", reason("Gia đình đề nghị")).andExpect(status().isOk());
        assertThat(json(portalGet(nMember, "/residents").andExpect(status().isOk())))
                .isEmpty();
        portalGet(nMember, "/residents/" + a + "/general").andExpect(status().isForbidden());
    }

    @Test
    void US5_5_otherResident_isRefused() throws Exception {
        portalGet(nMember, "/residents/" + b + "/general").andExpect(status().isForbidden());
        portalGet(mMember, "/residents/" + b + "/health/allergies-conditions").andExpect(status().isForbidden());
    }

    @Test
    void US5_2_general_forEveryRelative_currentValuesOnly() throws Exception {
        JsonNode general =
                json(portalGet(nMember, "/residents/" + a + "/general").andExpect(status().isOk()));
        assertThat(general.get("residentId").asLong()).isEqualTo(a);
        assertThat(general.get("fullName").asString()).isEqualTo("Người cao tuổi thử");
        assertThat(general.get("status").asString()).isEqualTo("IN_RESIDENCE");
        assertThat(general.get("visits").isArray()).isTrue();
        assertThat(general.has("healthItems")).isFalse();
    }

    @Test
    void US5_1_health_needsRightAndConsent() throws Exception {
        portalGet(nMember, "/residents/" + a + "/health/allergies-conditions")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NO_HEALTH_VIEW_RIGHT"));
        portalGet(mMember, "/residents/" + a + "/health/allergies-conditions")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("RIGHT_NOT_EFFECTIVE"));

        consents.active(a, Set.of(mMember), false);
        JsonNode section = json(portalGet(mMember, "/residents/" + a + "/health/allergies-conditions")
                .andExpect(status().isOk()));
        assertThat(section.get("available").asBoolean()).isTrue();
        assertThat(section.get("items")).anySatisfy(i -> {
            assertThat(i.get("type").asString()).isEqualTo("CHRONIC_CONDITION");
            assertThat(i.get("text").asString()).isEqualTo("Tăng huyết áp");
        });
    }

    @Test
    void US5_4_contract_onlyRepresentative() throws Exception {
        JsonNode contract =
                json(portalGet(rMember, "/residents/" + a + "/contract").andExpect(status().isOk()));
        assertThat(contract.get("contracts").isArray()).isTrue();
        portalGet(mMember, "/residents/" + a + "/contract").andExpect(status().isForbidden());
        portalGet(nMember, "/residents/" + a + "/contract").andExpect(status().isForbidden());
    }

    @Test
    void FR_046_staffAccount_cannotUseFamilyPortal() throws Exception {
        mvc.perform(get("/api/v1/family-portal/residents").header("Authorization", staffSession(Role.OFFICE_STAFF)))
                .andExpect(status().isForbidden());
    }
}
