package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.belonging.service.HeldBelongingReminders;
import com.eldercare.backend.belongings.internal.belonging.service.HeldBelongingsHandoverPortImpl;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import com.eldercare.backend.staffing.api.spi.HeldBelongingView;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 013 T014 (US2; FR-014a, FR-028, research K-11): bản cài {@code HeldBelongingsHandoverPort} cho bàn giao ca của 008;
 * nhắc khi hết ca còn giữ đồ có giá trị; báo khi chuyển tầng còn đồ ở vị trí tầng cũ. Listener gọi trực tiếp trong
 * transaction để kiểm cả lần chạy lại (idempotent).
 */
class FR_014a_HeldBelongingsIT extends HandoverTestBase {

    @Autowired
    HeldBelongingsHandoverPortImpl port;

    @Autowired
    HeldBelongingReminders reminders;

    @Autowired
    TransactionTemplate tx;

    @Test
    void FR_014a_portListsValuableHeldBelongings_andStillHeldFollowsTransfers() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location);
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location);

        assertThat(port.heldBy(List.of(officeA.staffId())))
                .contains(new HeldBelongingView(phone, officeA.staffId(), resident, location))
                .noneMatch(v -> v.belongingId() == clothes);
        assertThat(port.stillHeld(List.of(phone), List.of(officeA.staffId()))).containsEntry(phone, true);
        getJson(tokenA, "/api/v1/staff/" + officeA.staffId() + "/held-belongings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + phone + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + clothes + ")]").doesNotExist());

        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(phone, ""),
                                ", \"storageLocationId\": " + location + ", \"recipientStaffId\": " + officeB.staffId()
                                        + ", \"reason\": \"Giao ca\""))
                .andExpect(status().isOk());

        assertThat(port.stillHeld(List.of(phone, -1L), List.of(officeA.staffId())))
                .containsEntry(phone, false)
                .containsEntry(-1L, false);
        assertThat(port.heldBy(List.of(officeB.staffId())))
                .extracting(HeldBelongingView::belongingId)
                .contains(phone);
    }

    @Test
    void FR_014a_shiftEnded_remindsHolderAndShiftLead_once() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long lead = users.staff(Role.NURSING_STAFF).staffId();
        long shift = onShift(lead, officeA.staffId());
        var endsAt = shifts.staffShifts(
                        officeA.staffId(), time.now(), time.now().plusSeconds(1))
                .getFirst()
                .endsAt();
        events.clear();

        tx.executeWithoutResult(s -> reminders.shiftEnded(new ShiftEnded(shift, null, "ENDED", endsAt)));
        tx.executeWithoutResult(s -> reminders.shiftEnded(new ShiftEnded(shift, null, "ENDED", endsAt)));

        assertThat(notices())
                .filteredOn(n -> n.eventType().equals("BELONGING_HELD_AT_SHIFT_END"))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.level()).contains(NotificationLevel.MEDIUM);
                    assertThat(n.recipientGroups())
                            .containsExactly(
                                    new RecipientGroup.Staff(officeA.staffId(), "HOLDER"),
                                    new RecipientGroup.Staff(lead, "SHIFT_LEAD"));
                });
        assertThat(port.heldBy(List.of(officeA.staffId())))
                .extracting(HeldBelongingView::belongingId)
                .contains(phone);
    }

    @Test
    void FR_028_bedTransferToAnotherFloor_notifiesOfficeAndNurse_once_withoutChangingBelongings() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long oldFloor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)"
                        + " VALUES ('M12.STORAGE_LOCATION', 'M12', ?, 0, 'ACTIVE', JSON_OBJECT('floorAreaId', ?))",
                "Tủ tầng " + oldFloor,
                oldFloor);
        long floorLocation = jdbc.queryForObject(
                "SELECT MAX(id) FROM catalog_item WHERE catalog_type = 'M12.STORAGE_LOCATION'", Long.class);
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 3", floorLocation);
        int before = version(clothes);
        long next = TestUsers.nextId();
        events.clear();

        tx.executeWithoutResult(s -> reminders.bedTransferred(
                new BedTransferred(resident, TestUsers.nextId(), next, oldFloor, oldFloor, time.now())));
        assertThat(notices()).isEmpty();
        var moved = new BedTransferred(resident, TestUsers.nextId(), next, oldFloor, TestUsers.nextId(), time.now());
        tx.executeWithoutResult(s -> reminders.bedTransferred(moved));
        tx.executeWithoutResult(s -> reminders.bedTransferred(moved));

        assertThat(notices()).singleElement().satisfies(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGINGS_AT_PREVIOUS_FLOOR");
            assertThat(n.level()).contains(NotificationLevel.LOW);
            assertThat(n.recipientGroups())
                    .containsExactly(
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                            new RecipientGroup.AssignedNurse(resident, new Substitution.Default()));
        });
        assertThat(version(clothes)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT storage_location_id FROM belonging WHERE id = ?", Long.class, clothes))
                .isEqualTo(floorLocation);
    }
}
