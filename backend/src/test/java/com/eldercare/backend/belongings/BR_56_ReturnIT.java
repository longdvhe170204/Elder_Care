package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 013 T016 (US3; BR-M12-04, FR-016 → FR-019, SC-004; quickstart Q14 → Q17, Q21): lệnh Trả theo người nhận hợp lệ; lần
 * chặn trả còn lại sau khi lệnh bị từ chối; Điều dưỡng không trả đồ có giá trị; xác nhận người nhận khác dùng một lần.
 */
class BR_56_ReturnIT extends ReturnTestBase {

    @Test
    void Q14_representativeWithMatchingDocument_receivesValuableItem_withPhotoAndSignature() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long picker = representative(resident, "012345");
        long other = TestUsers.nextId();
        family.representative(other, resident);

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, true)),
                                familyRecipient(picker, "Người thân " + picker, "cccd", "012345"),
                                ", \"conditionOpinion\": \"Máy còn tốt\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.handovers[0].kind").value("RETURN"))
                .andExpect(jsonPath("$.belongings[0].status").value("RETURNED"));

        var row =
                jdbc.queryForMap("SELECT * FROM belonging_handover WHERE belonging_id = ? AND kind = 'RETURN'", phone);
        assertThat(row)
                .containsEntry("target_status", "RETURNED")
                .containsEntry("recipient_type", "FAMILY_MEMBER")
                .containsEntry("recipient_id", picker)
                .containsEntry("recipient_basis", "ENTITLED_RECIPIENT")
                .containsEntry("verification_result", "MATCHED")
                .containsEntry("giver_type", "STAFF")
                .containsEntry("giver_id", officeA.staffId())
                .containsEntry("condition_opinion", "Máy còn tốt")
                .containsEntry("valuable", true);
        assertThat(row.get("signature_file_id")).isNotNull();
        assertThat(jdbc.queryForMap(
                        "SELECT status, holder_type, holder_id, current_handover_id FROM belonging WHERE id = ?",
                        phone))
                .containsEntry("status", "RETURNED")
                .containsEntry("holder_type", null)
                .containsEntry("current_handover_id", latestHandover(phone));
        assertThat(blockedCount(resident)).isZero();
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGING_RETURNED");
            assertThat(n.level()).contains(NotificationLevel.LOW);
            assertThat(n.recipientGroups())
                    .contains(new RecipientGroup.FamilyMember(other, "REPRESENTATIVE"))
                    .doesNotContain(new RecipientGroup.FamilyMember(picker, "REPRESENTATIVE"));
        });
    }
}
