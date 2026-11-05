package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * 013 T016 (US3; BR-M12-04, FR-016 → FR-019, SC-004; quickstart Q14 → Q17, Q21): lệnh Trả theo người nhận hợp lệ; lần
 * chặn trả còn lại sau khi lệnh bị từ chối; Điều dưỡng không trả đồ có giá trị; xác nhận người nhận khác dùng một lần.
 */
class BR_M12_04_ReturnIT extends ReturnTestBase {

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

    @Test
    void FR_016_profileWithoutRegisteredDocument_recordsPresentedDocument_andAsksOfficeToComplete() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long picker = TestUsers.nextId();
        family.representative(picker, resident);

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(glasses, false)),
                                familyRecipient(picker, "Người thân " + picker, "CCCD", "777"),
                                ""))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForMap(
                        "SELECT verification_result, recipient_id_document_type, recipient_id_document_number"
                                + " FROM belonging_handover WHERE belonging_id = ? AND kind = 'RETURN'",
                        glasses))
                .containsEntry("verification_result", "NO_DOCUMENT_ON_FILE_CROSS_CHECKED")
                .containsEntry("recipient_id_document_type", "CCCD")
                .containsEntry("recipient_id_document_number", "777");
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("FAMILY_ID_DOCUMENT_MISSING");
            assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        });
    }

    @Test
    void Q15_SC_004_returnToPersonWithoutRight_isBlocked_andTheBlockIsRecorded() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 3", location());
        long stranger = TestUsers.nextId();
        family.relate(stranger, resident);

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, true), returnItem(clothes, false)),
                                familyRecipient(stranger, "Người thân " + stranger, "CCCD", "123"),
                                ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RETURN_BLOCKED"));

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(statusOf(clothes)).isEqualTo("HELD");
        assertThat(blockedCount(resident)).as("mỗi đồ trong lệnh một lần chặn").isEqualTo(2);
        assertThat(lastBlockReason(resident)).isEqualTo("NOT_ENTITLED_TO_RECEIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE resident_id = ? AND kind = 'RETURN'",
                        Integer.class,
                        resident))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT current_handover_id FROM belonging WHERE id = ?", Long.class, phone))
                .as("lần chặn không đổi bản ghi hiện hành của đồ")
                .isEqualTo(jdbc.queryForObject(
                        "SELECT MIN(id) FROM belonging_handover WHERE belonging_id = ?", Long.class, phone));
    }

    @Test
    void FR_016_documentMismatch_isBlocked_andRecordedWithItsReason() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long picker = representative(resident, "012345");

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, true)),
                                familyRecipient(picker, "Người thân " + picker, "CCCD", "999999"),
                                ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RETURN_BLOCKED"));

        assertThat(lastBlockReason(resident)).isEqualTo("ID_DOCUMENT_MISMATCH");
        assertThat(statusOf(phone)).isEqualTo("HELD");
    }

    @Test
    void Q16_residentReceivesOwnItem_withoutConfirmation_exceptConsentTypes() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 3", location());
        long cash = intake(resident, "CASH", "\"cashAmount\": 500000", location());
        String self = "{\"type\": \"RESIDENT\"}";

        giveBack(tokenA, returnBody(List.of(returnItem(cash, true)), self, ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RETURN_BLOCKED"));
        assertThat(lastBlockReason(resident)).isEqualTo("NOT_ENTITLED_TO_RECEIVE");

        giveBack(tokenA, returnBody(List.of(returnItem(clothes, false)), self, ""))
                .andExpect(status().isCreated());
        assertThat(statusOf(clothes)).isEqualTo("RETURNED");
        assertThat(jdbc.queryForMap(
                        "SELECT recipient_type, recipient_id, recipient_basis FROM belonging_handover"
                                + " WHERE belonging_id = ? AND kind = 'RETURN'",
                        clothes))
                .containsEntry("recipient_type", "RESIDENT")
                .containsEntry("recipient_id", resident)
                .containsEntry("recipient_basis", "RESIDENT_Q253_EXCEPTION");
    }

    @Test
    void FR_018_returnFromInUse_recordsStaffAsGiver_andNoteTakenBackFromResident() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 3", location());
        handover(tokenA, command("GIVE_FOR_USE", item(clothes, ""), "")).andExpect(status().isOk());
        long picker = representative(resident, "012345");

        giveBack(
                        tokenB,
                        returnBody(
                                List.of(returnItem(clothes, false)),
                                familyRecipient(picker, "Người thân " + picker, "CCCD", "012345"),
                                ""))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForMap(
                        "SELECT giver_type, giver_id, note FROM belonging_handover WHERE belonging_id = ?"
                                + " AND kind = 'RETURN'",
                        clothes))
                .containsEntry("giver_type", "STAFF")
                .containsEntry("giver_id", officeB.staffId())
                .containsEntry("note", "Thu từ người cao tuổi");
    }

    @Test
    void FR_017_Q17_confirmationIsUsedOnce_secondReturnWithItIsBlocked() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long confirmation = confirmation(resident, "Trần Thị N", "999", phone, glasses);
        postJson(
                        portalSession(representative),
                        "/api/v1/family-portal/recipient-confirmations/" + confirmation + "/confirm",
                        "{}")
                .andExpect(status().isOk());
        assertThat(confirmationStatus(confirmation)).isEqualTo("EFFECTIVE");

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, true)),
                                otherRecipient("Trần Thị N", "999"),
                                ", \"recipientConfirmationId\": " + confirmation))
                .andExpect(status().isCreated());

        assertThat(confirmationStatus(confirmation)).isEqualTo("USED");
        assertThat(jdbc.queryForObject(
                        "SELECT recipient_confirmation_id FROM belonging_handover WHERE belonging_id = ?"
                                + " AND kind = 'RETURN'",
                        Long.class,
                        phone))
                .isEqualTo(confirmation);
        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(glasses, false)),
                                otherRecipient("Trần Thị N", "999"),
                                ", \"recipientConfirmationId\": " + confirmation))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECIPIENT_CONFIRMATION"));
        assertThat(lastBlockReason(resident)).isEqualTo("INVALID_RECIPIENT_CONFIRMATION");
        assertThat(statusOf(glasses)).isEqualTo("HELD");
    }

    @Test
    void FR_017_confirmationForAnotherPerson_orAnotherItem_doesNotCover() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long confirmation = confirmation(resident, "Trần Thị N", "999", phone);
        postJson(
                        portalSession(representative),
                        "/api/v1/family-portal/recipient-confirmations/" + confirmation + "/confirm",
                        "{}")
                .andExpect(status().isOk());

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, true)),
                                otherRecipient("Người khác", "111"),
                                ", \"recipientConfirmationId\": " + confirmation))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECIPIENT_CONFIRMATION"));
        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(glasses, false)),
                                otherRecipient("Trần Thị N", "999"),
                                ", \"recipientConfirmationId\": " + confirmation))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECIPIENT_CONFIRMATION"));
        assertThat(confirmationStatus(confirmation)).isEqualTo("EFFECTIVE");
    }

    @Test
    void Q21_FR_016a_nurseReturningValuableItem_isBlockedWholeCommand_plainItemsAreAllowed() throws Exception {
        long resident = resident("IN_RESIDENCE");
        assignNurse(resident);
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location());
        long picker = representative(resident, "012345");
        String recipient = familyRecipient(picker, "Người thân " + picker, "CCCD", "012345");
        // Điều dưỡng chỉ trả đồ mình giữ: chuyển giữ cho Điều dưỡng trước
        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(phone, "") + ", " + item(clothes, ""),
                                ", \"recipientStaffId\": " + nurseUser.staffId() + ", \"storageLocationId\": "
                                        + location() + ", \"reason\": \"Cho trực\""))
                .andExpect(status().isOk());

        giveBack(nurseToken, returnBody(List.of(returnItem(phone, true), returnItem(clothes, false)), recipient, ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NURSING_STAFF_VALUABLE_ITEM"));

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(statusOf(clothes)).isEqualTo("HELD");
        assertThat(blockedCount(resident)).isEqualTo(2);
        assertThat(lastBlockReason(resident)).isEqualTo("NURSING_STAFF_VALUABLE_ITEM");

        giveBack(nurseToken, returnBody(List.of(returnItem(clothes, false)), recipient, ""))
                .andExpect(status().isCreated());
        assertThat(statusOf(clothes)).isEqualTo("RETURNED");
    }

    @Test
    void FR_016a_nurse_cannotReturnByConfirmation() throws Exception {
        long resident = resident("IN_RESIDENCE");
        assignNurse(resident);
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location());
        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(clothes, ""),
                                ", \"recipientStaffId\": " + nurseUser.staffId() + ", \"storageLocationId\": "
                                        + location() + ", \"reason\": \"Cho trực\""))
                .andExpect(status().isOk());
        long representative = representative(resident, "012345");
        long confirmation = confirmation(resident, "Trần Thị N", "999", clothes);
        postJson(
                        portalSession(representative),
                        "/api/v1/family-portal/recipient-confirmations/" + confirmation + "/confirm",
                        "{}")
                .andExpect(status().isOk());

        giveBack(
                        nurseToken,
                        returnBody(
                                List.of(returnItem(clothes, false)),
                                otherRecipient("Trần Thị N", "999"),
                                ", \"recipientConfirmationId\": " + confirmation))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RETURN_BLOCKED"));
        assertThat(confirmationStatus(confirmation)).isEqualTo("EFFECTIVE");
    }

    @Test
    void BR_M12_05_valuableItemReturnedWithoutPhoto_isRejected_withoutRecordingABlock() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long picker = representative(resident, "012345");

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, false)),
                                familyRecipient(picker, "Người thân " + picker, "CCCD", "012345"),
                                ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PHOTO_REQUIRED"));
        assertThat(blockedCount(resident)).isZero();
        assertThat(statusOf(phone)).isEqualTo("HELD");
    }

    @Test
    void FR_019_signatureIsRequired_exceptResidentWhoCannotSign() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location());
        long picker = representative(resident, "012345");
        String byFamily = familyRecipient(picker, "Người thân " + picker, "CCCD", "012345");

        giveBack(tokenA, "{\"items\": [" + returnItem(clothes, false) + "], \"recipient\": " + byFamily + "}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
        giveBack(
                        tokenA,
                        "{\"items\": [" + returnItem(clothes, false) + "], \"recipient\": {\"type\": \"RESIDENT\"},"
                                + " \"noSignatureReason\": \"Người cao tuổi không còn khả năng ký\"}")
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForMap(
                        "SELECT signature_file_id, no_signature_reason FROM belonging_handover WHERE belonging_id = ?"
                                + " AND kind = 'RETURN'",
                        clothes))
                .containsEntry("signature_file_id", null)
                .containsEntry("no_signature_reason", "Người cao tuổi không còn khả năng ký");
    }

    /** 013-2 (a), FR-012: trả một phần tiền mặt tách phần trả thành đồ mới Đã trả, phần còn lại giữ Đang giữ. */
    @Test
    void FR_012_partialCashReturn_splitsTheReturnedAmount_andTheRestStaysHeld() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long cash = intake(resident, "CASH", "\"cashAmount\": 2000000", location());
        long picker = representative(resident, "012345");
        String recipient = familyRecipient(picker, "Người thân " + picker, "CCCD", "012345");

        giveBack(tokenA, returnBody(List.of(partialItem(cash, "3000000")), recipient, ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
        giveBack(tokenA, returnBody(List.of(partialItem(cash, "500000")), recipient, ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.handovers.length()").value(1))
                .andExpect(jsonPath("$.handovers[0].kind").value("RETURN"));

        long part = jdbc.queryForObject("SELECT id FROM belonging WHERE original_belonging_id = ?", Long.class, cash);
        assertThat(jdbc.queryForMap("SELECT status, cash_amount, holder_type FROM belonging WHERE id = ?", part))
                .containsEntry("status", "RETURNED")
                .containsEntry("cash_amount", new java.math.BigDecimal("500000"))
                .containsEntry("holder_type", null);
        assertThat(jdbc.queryForMap("SELECT status, cash_amount FROM belonging WHERE id = ?", cash))
                .containsEntry("status", "HELD")
                .containsEntry("cash_amount", new java.math.BigDecimal("1500000"));
        assertThat(jdbc.queryForMap(
                        "SELECT cash_amount, recipient_id, target_status FROM belonging_handover WHERE belonging_id = ?"
                                + " AND kind = 'RETURN'",
                        part))
                .containsEntry("cash_amount", new java.math.BigDecimal("500000"))
                .containsEntry("recipient_id", picker)
                .containsEntry("target_status", "RETURNED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ? AND kind = 'RETURN'",
                        Integer.class,
                        cash))
                .isZero();
    }

    /** Một đồ của lệnh Trả với số lượng (số tiền với tiền mặt) và ảnh (đồ có giá trị). */
    private String partialItem(long belongingId, String quantity) {
        return "{\"belongingId\": " + belongingId + ", \"version\": " + version(belongingId) + ", \"quantity\": "
                + quantity + ", \"condition\": {\"levelId\": " + levelId("SIGNS_OF_USE") + "}, \"photoFileIds\": ["
                + photo() + "]}";
    }

    @Test
    void FR_019_returnedBelonging_cannotBeReturnedAgain_andRecordsCannotBeChanged() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location());
        long picker = representative(resident, "012345");
        String recipient = familyRecipient(picker, "Người thân " + picker, "CCCD", "012345");
        String body = returnBody(List.of(returnItem(clothes, false)), recipient, "");
        giveBack(tokenA, body).andExpect(status().isCreated());

        giveBack(tokenA, body).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ? AND kind = 'RETURN'",
                        Integer.class,
                        clothes))
                .isOne();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                        "UPDATE belonging_handover SET signature_file_id = 1 WHERE belonging_id = ?", clothes))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void K_03_twoConcurrentReturnsOfTheSameBelonging_oneSucceeds() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long picker = representative(resident, "012345");
        String body = returnBody(
                List.of(returnItem(phone, true)),
                familyRecipient(picker, "Người thân " + picker, "CCCD", "012345"),
                "");
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> call = () -> {
                start.await();
                return giveBack(tokenA, body).andReturn().getResponse().getStatus();
            };
            Future<Integer> first = pool.submit(call);
            Future<Integer> second = pool.submit(call);
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ? AND kind = 'RETURN'",
                        Integer.class,
                        phone))
                .isOne();
        assertThat(statusOf(phone)).isEqualTo("RETURNED");
    }
}
