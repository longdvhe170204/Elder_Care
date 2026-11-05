package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * 012 T009 (FR-075, FR-003): khi người cao tuổi sang trạng thái cuối, quan hệ giữ Hiệu lực tới khi 002 khóa tài khoản
 * theo CFG-M01-04; 012 không chặn lệnh trạng thái cuối và DBR-02 không còn áp. Hủy yêu cầu chờ, ngoại lệ đón, lượt thăm
 * tương lai bổ sung cùng US3, US2, US4.
 */
class FR_075_FinalStatusIT extends FamilyCommandTestBase {

    @Autowired
    FamilyRightsApi rights;

    @Test
    void FR_075_relationshipsStayActive_andFinalCommandsAreNotBlocked() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Trần R");
        long r = relate(a, rMember, true, true);

        for (StatusCommand command : new StatusCommand[] {StatusCommand.RECORD_DEATH, StatusCommand.END_RESIDENCY}) {
            assertThat(familyParticipant()
                            .checkConditions(new CommandContext(
                                    a, ResidentStatus.IN_RESIDENCE, command, null, time.now(), null)))
                    .isEmpty();
        }
        jdbc.update("UPDATE resident SET status = 'DECEASED' WHERE id = ?", a);

        JsonNode list = getJson("/api/v1/residents/" + a + "/relationships");
        assertThat(list).singleElement().satisfies(rel -> {
            assertThat(rel.get("id").asLong()).isEqualTo(r);
            assertThat(rel.get("status").asString()).isEqualTo("ACTIVE");
            assertThat(rel.get("representative").asBoolean()).isTrue();
        });
        assertThat(rights.hasActiveRelationship(rMember, a)).isTrue();
    }

    @Test
    void FR_003_dbr02NoLongerApplies_afterFinalStatus() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Lê R"), true, true);
        jdbc.update("UPDATE resident SET status = 'RESIDENCY_ENDED' WHERE id = ?", a);

        command(r, "end", reason("Gia đình đề nghị sau khi kết thúc lưu trú")).andExpect(status().isOk());

        assertThat(relationshipStatus(r)).isEqualTo("ENDED");
    }
}
