package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * 013 T012 (US2; BR-M12-01, FR-011 → FR-015a, SC-001; quickstart Q5 → Q8, Q10): lệnh bàn giao tạo đúng một bản ghi mỗi
 * đồ và cập nhật cột suy ra cùng lần; tách đồ, đếm thiếu; người giao là người giữ; bản ghi không sửa được.
 */
class BR_M12_01_HandoverIT extends HandoverTestBase {

    @Test
    void Q5_FR_012_partialTransfer_splitsIntoNewBelonging_totalUnchanged() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long floor1 = location();
        long floor2 = location();
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 10", floor1);

        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(clothes, ", \"quantity\": 4"),
                                ", \"storageLocationId\": " + floor2 + ", \"reason\": \"Chuyển lên tủ tầng 2\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handovers.length()").value(1))
                .andExpect(jsonPath("$.handovers[0].kind").value("TRANSFER_CUSTODY"));

        var all = sameIntake(clothes);
        assertThat(all).hasSize(2);
        long split = all.get(1);
        assertThat(quantityOf(clothes)).isEqualByComparingTo("6");
        assertThat(quantityOf(split)).isEqualByComparingTo("4");
        assertThat(intakeTotal(clothes)).isEqualByComparingTo("10");
        assertThat(jdbc.queryForMap(
                        "SELECT status, storage_location_id, original_belonging_id, current_handover_id FROM belonging"
                                + " WHERE id = ?",
                        split))
                .containsEntry("status", "HELD")
                .containsEntry("storage_location_id", floor2)
                .containsEntry("original_belonging_id", clothes)
                .containsEntry("current_handover_id", latestHandover(split));
        assertThat(jdbc.queryForObject("SELECT storage_location_id FROM belonging WHERE id = ?", Long.class, clothes))
                .isEqualTo(floor1);
        assertThat(jdbc.queryForList(
                        "SELECT kind FROM belonging_handover WHERE belonging_id = ? ORDER BY id", String.class, split))
                .containsExactly("TRANSFER_CUSTODY");
    }

    @Test
    void Q6_FR_013_countedShortfall_splitsMissingPart_andReportsItLost_inTheSameCommand() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 6", location);
        handover(tokenA, command("GIVE_FOR_USE", item(clothes, ""), "")).andExpect(status().isOk());

        handover(
                        tokenA,
                        command(
                                "TAKE_BACK",
                                item(clothes, ", \"countedQuantity\": 5"),
                                ", \"storageLocationId\": " + location))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        assertThat(sameIntake(clothes)).hasSize(1);

        handover(
                        tokenA,
                        command(
                                "TAKE_BACK",
                                item(clothes, ", \"countedQuantity\": 5"),
                                ", \"storageLocationId\": " + location + ", \"reason\": \"Đếm thiếu một bộ\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handovers.length()").value(2));

        var all = sameIntake(clothes);
        assertThat(all).hasSize(2);
        long lost = all.get(1);
        assertThat(statusOf(clothes)).isEqualTo("HELD");
        assertThat(quantityOf(clothes)).isEqualByComparingTo("5");
        assertThat(statusOf(lost)).isEqualTo("LOST");
        assertThat(quantityOf(lost)).isEqualByComparingTo("1");
        assertThat(intakeTotal(clothes)).isEqualByComparingTo("6");
        assertThat(jdbc.queryForList(
                        "SELECT kind FROM belonging_handover WHERE belonging_id = ? ORDER BY id", String.class, lost))
                .containsExactly("REPORT_LOST");
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGING_LOST");
            assertThat(n.level()).contains(NotificationLevel.MEDIUM);
            assertThat(n.recipientGroups())
                    .contains(
                            new RecipientGroup.PrimaryContact(resident),
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        });
    }

    @Test
    void Q7_FR_014_giverMustBeHolder_unlessHolderIsReallyAbsent() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location);
        onShift(null, officeA.staffId());
        String transfer = ", \"storageLocationId\": " + location() + ", \"reason\": \"Đổi tủ\"";

        handover(tokenB, command("TRANSFER_CUSTODY", item(phone, ""), transfer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GIVER_NOT_HOLDER"));
        handover(tokenB, command("TRANSFER_CUSTODY", item(phone, ""), transfer + ", \"holderAbsent\": true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("HOLDER_NOT_ABSENT"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ?", Integer.class, phone))
                .isEqualTo(1);
    }

    @Test
    void Q8_FR_014_officeStaffActsForAbsentHolder_withReason_oldHolderIsNotified() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long newLocation = location();

        handover(
                        tokenB,
                        command(
                                "TRANSFER_CUSTODY",
                                item(phone, ""),
                                ", \"storageLocationId\": " + newLocation + ", \"recipientStaffId\": "
                                        + officeB.staffId() + ", \"holderAbsent\": true,"
                                        + " \"reason\": \"Người giữ vắng mặt\""))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForMap(
                        "SELECT giver_type, giver_id, recipient_id, holder_absent FROM belonging_handover WHERE id = ?",
                        latestHandover(phone)))
                .containsEntry("giver_type", "STAFF")
                .containsEntry("giver_id", officeA.staffId())
                .containsEntry("recipient_id", officeB.staffId())
                .containsEntry("holder_absent", true);
        assertThat(jdbc.queryForMap("SELECT holder_id, storage_location_id FROM belonging WHERE id = ?", phone))
                .containsEntry("holder_id", officeB.staffId())
                .containsEntry("storage_location_id", newLocation);
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGING_HANDOVER_HOLDER_ABSENT");
            assertThat(n.level()).contains(NotificationLevel.LOW);
            assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.Staff(officeA.staffId(), "HOLDER"));
        });
    }

    @Test
    void FR_014_inactiveAccount_countsAsAbsent_evenOnShift() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        onShift(null, officeA.staffId());
        jdbc.update("UPDATE account SET status = 'INACTIVE' WHERE id = ?", officeA.accountId());

        handover(
                        tokenB,
                        command(
                                "TRANSFER_CUSTODY",
                                item(phone, ""),
                                ", \"storageLocationId\": " + location() + ", \"recipientStaffId\": "
                                        + officeB.staffId() + ", \"holderAbsent\": true,"
                                        + " \"reason\": \"Tài khoản người giữ đã khóa\""))
                .andExpect(status().isOk());
    }

    @Test
    void Q10_FR_015_worseCondition_needsHandling_acknowledgeSendsNoNotice() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location);
        handover(tokenA, command("GIVE_FOR_USE", item(glasses, ""), "")).andExpect(status().isOk());
        String worse = ", \"condition\": {\"levelId\": " + levelId("PARTLY_DAMAGED")
                + ", \"description\": \"Trầy tròng kính\"}";

        handover(tokenA, command("TAKE_BACK", item(glasses, worse), ", \"storageLocationId\": " + location))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_HANDLING_REQUIRED"));
        events.clear();
        handover(
                        tokenA,
                        command(
                                "TAKE_BACK",
                                item(glasses, worse),
                                ", \"storageLocationId\": " + location + ", \"conditionHandling\": \"ACKNOWLEDGE\""))
                .andExpect(status().isOk());

        assertThat(statusOf(glasses)).isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT condition_level_id FROM belonging WHERE id = ?", Long.class, glasses))
                .isEqualTo(levelId("PARTLY_DAMAGED"));
        assertThat(notices()).isEmpty();
    }

    @Test
    void FR_015a_consentType_needsEffectiveConsent_andConsentExpiresWhenLost() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long cashNoConsent = intake(resident, "CASH", "\"cashAmount\": 500000", location);
        handover(tokenA, command("GIVE_FOR_USE", item(cashNoConsent, ""), ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SELF_CUSTODY_CONSENT_REQUIRED"));

        long representative = TestUsers.nextId();
        family.representative(representative, resident);
        String giveNow = "{\"typeId\": " + typeId("CASH") + ", \"description\": \"Tiền mặt\","
                + " \"cashAmount\": 2000000, \"condition\": {\"levelId\": " + levelId("GOOD") + "},"
                + " \"giver\": {\"type\": \"FAMILY_MEMBER\", \"id\": " + representative + "},"
                + " \"at\": \"2026-11-05T08:00:00+07:00\", \"giveForUseNow\": true, \"photoFileIds\": [" + photo()
                + "], \"consentSignedFormFileId\": " + photo() + "}";
        long cash = id(postJson(tokenA, "/api/v1/residents/" + resident + "/belongings", giveNow)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        handover(tokenA, command("TAKE_BACK", item(cash, ""), ", \"storageLocationId\": " + location))
                .andExpect(status().isOk());
        handover(tokenA, command("GIVE_FOR_USE", item(cash, ", \"quantity\": 500000"), ""))
                .andExpect(status().isOk());

        long inUse = sameIntake(cash).get(1);
        assertThat(statusOf(inUse)).isEqualTo("IN_USE");
        assertThat(jdbc.queryForObject("SELECT cash_amount FROM belonging WHERE id = ?", BigDecimal.class, cash))
                .isEqualByComparingTo("1500000");

        handover(tokenA, command("REPORT_LOST", item(cash, ""), ", \"reason\": \"Không thấy trong tủ\""))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM simple_record WHERE kind = 'M12.SELF_CUSTODY_CONSENT' AND entity_id = ?",
                        String.class,
                        cash))
                .isEqualTo("EXPIRED");
    }

    @Test
    void FR_015a_turningOnConsentFlag_notifiesInUseBelongingsWithoutConsent() throws Exception {
        long resident = resident("IN_RESIDENCE");
        String name = unique("Ví da");
        long type = id(postJson(manager, "/api/v1/belongings/types", "{\"name\": \"" + name + "\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String body = "{\"typeId\": " + type + ", \"description\": \"Ví\", \"quantity\": 1,"
                + " \"condition\": {\"levelId\": " + levelId("GOOD") + "}, \"giver\": {\"type\": \"RESIDENT\"},"
                + " \"at\": \"2026-11-05T08:00:00+07:00\", \"giveForUseNow\": true}";
        long wallet = id(postJson(tokenA, "/api/v1/residents/" + resident + "/belongings", body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        events.clear();

        putJson(
                        manager,
                        "/api/v1/belongings/types/" + type,
                        "{\"name\": \"" + name + "\", \"requiresConsentForUse\": true, \"reason\": \"Ví có tiền\"}")
                .andExpect(status().isOk());

        assertThat(statusOf(wallet)).isEqualTo("IN_USE");
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("SELF_CUSTODY_CONSENT_REQUIRED_IN_USE");
            assertThat(n.level()).contains(NotificationLevel.MEDIUM);
            assertThat(n.sourceRecord().id()).isEqualTo(Long.toString(wallet));
            assertThat(n.recipientGroups())
                    .contains(
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                            new RecipientGroup.AssignedNurse(
                                    resident, new com.eldercare.backend.common.notification.Substitution.Default()));
        });
    }

    @Test
    void FR_027_giveForUse_blockedInFinalState_butTransferStillAllowed() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location());
        jdbc.update("UPDATE resident SET status = 'DECEASED' WHERE id = ?", resident);

        handover(tokenA, command("GIVE_FOR_USE", item(clothes, ""), ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_FINAL_STATE"));
        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(clothes, ""),
                                ", \"storageLocationId\": " + location() + ", \"reason\": \"Gom đồ chờ trả\""))
                .andExpect(status().isOk());
    }

    @Test
    void BR_M12_01_handoverRecords_cannotBeUpdatedOrDeleted() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long record = latestHandover(clothes);

        assertThatThrownBy(() -> jdbc.update("UPDATE belonging_handover SET reason = 'sửa' WHERE id = ?", record))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM belonging_handover WHERE id = ?", record))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_029_managerHasViewOnly_cannotHandOver() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());

        handover(manager, command("GIVE_FOR_USE", item(clothes, ""), "")).andExpect(status().isForbidden());
    }
}
