package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 012 T026 ({@code FR_048_NhatKyXemIT}; FR-048, NFR-08, Q-352; bổ sung 2026-10-18): mỗi lần người thân mở một phần sức
 * khỏe có dữ liệu được ghi nhật ký (người thân, người cao tuổi, phần, nguồn cổng, thời điểm); Quản lý viện tra cứu, người
 * thân và vai trò khác không tra được. Người không có quyền xem sức khỏe không có dòng nhật ký. Phần chưa có nguồn (007,
 * 011, công thức ăn uống của 005) báo chưa có dữ liệu, không trả số, không ghi nhật ký.
 */
class FR_048_HealthViewLogIT extends FamilyCommandTestBase {

    long a;
    long mMember;
    long nMember;

    @BeforeEach
    void viewers() {
        a = resident(ResidentStatus.IN_RESIDENCE);
        mMember = fixtures.familyMember("Lê M", "0906000001");
        nMember = fixtures.familyMember("Phạm N", "0906000002");
        fixtures.relate(mMember, a, 1, true, true, FamilyRight.VIEW_HEALTH);
        fixtures.relate(nMember, a, 2, false, false);
        consents.active(a, Set.of(mMember), false);
    }

    private JsonNode logs(String authorization) throws Exception {
        return JSON.readTree(
                mvc.perform(get("/api/v1/health-view-logs?residentId=" + a).header("Authorization", authorization))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void FR_048_eachOpenedSection_isLogged_managerQueries() throws Exception {
        String m = familySession(mMember);
        mvc.perform(get("/api/v1/family-portal/residents/" + a + "/health/allergies-conditions")
                        .header("Authorization", m))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/family-portal/residents/" + a + "/health/medications")
                        .header("Authorization", m))
                .andExpect(status().isOk());

        JsonNode rows = logs(staffSession(Role.NURSING_HOME_MANAGER));
        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("familyMemberId").asLong()).isEqualTo(mMember);
            assertThat(r.get("residentId").asLong()).isEqualTo(a);
            assertThat(r.get("source").asString()).isEqualTo("PORTAL");
            assertThat(r.has("at")).isTrue();
        });
        assertThat(rows)
                .extracting(r -> r.get("section").asString())
                .containsExactlyInAnyOrder("ALLERGIES_CONDITIONS", "MEDICATIONS");

        mvc.perform(get("/api/v1/health-view-logs?residentId=" + a).header("Authorization", m))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/health-view-logs?residentId=" + a)
                        .header("Authorization", staffSession(Role.OFFICE_STAFF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void Q_352_noHealthRight_noLogLine() throws Exception {
        mvc.perform(get("/api/v1/family-portal/residents/" + a + "/health/incidents")
                        .header("Authorization", familySession(nMember)))
                .andExpect(status().isForbidden());
        assertThat(logs(staffSession(Role.NURSING_HOME_MANAGER))).isEmpty();
    }

    @Test
    void FR_046_sourceNotAvailable_saysSo_noNumbers_noLog() throws Exception {
        String m = familySession(mMember);
        for (String section : new String[] {"eating", "diet", "vital-signs"}) {
            JsonNode body = JSON.readTree(mvc.perform(get("/api/v1/family-portal/residents/" + a + "/health/" + section)
                            .header("Authorization", m))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(body.get("available").asBoolean()).as(section).isFalse();
            assertThat(body.get("items")).as(section).isEmpty();
        }
        assertThat(logs(staffSession(Role.NURSING_HOME_MANAGER))).isEmpty();
    }
}
