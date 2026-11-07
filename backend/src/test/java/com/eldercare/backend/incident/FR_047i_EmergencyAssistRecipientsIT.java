package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-047i, US13 #1 → #3 (T097; quickstart H1 → H3): người nhận theo tầng trong ca (Bác sĩ trực, Người phụ trách ca,
 * nhân viên trong ca); chưa có tầng thì Người phụ trách ca của mọi tầng đang có ca; chọn tầng sau thì gửi thêm nhóm của
 * tầng; không có nhóm người thân.
 */
class FR_047i_EmergencyAssistRecipientsIT extends AssistTestBase {

    @Test
    void H1_recipientsOfShiftFloorWithoutFamily() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());

        assertThat(assistCreatedGroups(presser))
                .contains(
                        new RecipientGroup.OnDutyDoctor(new Substitution.Default()),
                        new RecipientGroup.ShiftLead(floor, new Substitution.Default()),
                        new RecipientGroup.OnShiftStaff(floor, Set.of(), Set.of(), new Substitution.Default()))
                .doesNotContain(new RecipientGroup.ShiftLead(otherFloor, new Substitution.Default()))
                .noneMatch(group -> group instanceof RecipientGroup.PrimaryContact
                        || group instanceof RecipientGroup.Representative
                        || group instanceof RecipientGroup.FamilyWithRight);
    }

    @Test
    void H3_noFloorThenChosenFloorAddsFloorGroups() throws Exception {
        var roaming = users.staff(com.eldercare.backend.common.Role.OFFICE_STAFF);
        onShift(roaming, null);

        pressAssist(roaming).andExpect(status().isCreated());
        long alertId = assistAlert(roaming);
        assertThat(assistCreatedGroups(roaming))
                .contains(
                        new RecipientGroup.ShiftLead(floor, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(otherFloor, new Substitution.Default()),
                        new RecipientGroup.OnDutyDoctor(new Substitution.Default()))
                .noneMatch(group -> group instanceof RecipientGroup.OnShiftStaff);
        assertThat(jdbc.queryForObject("SELECT floor_area_id FROM alert WHERE id = ?", Long.class, alertId))
                .isNull();

        details(roaming, alertId, "{\"floorAreaId\":" + otherFloor + "}").andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT floor_area_id FROM alert WHERE id = ?", Long.class, alertId))
                .isEqualTo(otherFloor);
        assertThat(detailGroups())
                .contains(
                        new RecipientGroup.ShiftLead(otherFloor, new Substitution.Default()),
                        new RecipientGroup.OnShiftStaff(otherFloor, Set.of(), Set.of(), new Substitution.Default()));
    }
}
