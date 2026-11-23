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

    /**
     * BF-14 test web 2026-11-23: trả một phần (FR-012) thì phần tách sang Đã trả không còn người giữ như khi trả cả đồ (data-model
     * mục 3); trước đây phần tách mang người nhận làm người giữ, trả cho người khác (không có mã) thì 500 {@code
     * ck_belonging_holder}.
     */
    @Test
    void FR_012_partialReturn_returnedPartHasNoHolder_alsoForOtherPerson() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 10", location());
        long picker = representative(resident, "012345");
        String partOf4 = "{\"belongingId\": " + clothes
                + ", \"version\": %d, \"quantity\": 4, \"condition\": {\"levelId\": " + levelId("SIGNS_OF_USE") + "}}";

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(partOf4.formatted(version(clothes))),
                                familyRecipient(picker, "Người thân " + picker, "CCCD", "012345"),
                                ""))
                .andExpect(status().isCreated());

        long confirmationId = confirmation(resident, "Trần Văn Nhận", "CCCD 777", clothes);
        postJson(
                        portalSession(picker),
                        "/api/v1/family-portal/recipient-confirmations/" + confirmationId + "/confirm",
                        "{}")
                .andExpect(status().isOk());
        giveBack(
                        tokenA,
                        returnBody(
                                List.of(partOf4.formatted(version(clothes))),
                                otherRecipient("Trần Văn Nhận", "777"),
                                ", \"recipientConfirmationId\": " + confirmationId))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForList(
                        "SELECT holder_type, holder_id FROM belonging WHERE original_belonging_id = ? AND status = 'RETURNED'",
                        clothes))
                .hasSize(2)
                .allSatisfy(row ->
                        assertThat(row).containsEntry("holder_type", null).containsEntry("holder_id", null));
        assertThat(jdbc.queryForObject(
                        "SELECT quantity FROM belonging WHERE id = ?", java.math.BigDecimal.class, clothes))
                .isEqualByComparingTo("2");
    }
}
