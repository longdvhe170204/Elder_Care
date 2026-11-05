package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 012 T034 ({@code FR_040_GioiHanOLaiIT}; quickstart Q30; US8 kịch bản 4, 6; FR-040, FR-041, FR-045; Phụ lục 27 chú thích
 * 20): mỗi người cao tuổi tối đa CFG-M10-10 (mặc định 1) lượt Đã xác nhận hoặc Đang ở lại chồng thời gian; một người thân
 * không có hai lượt mở chồng; khoanh vùng, không Đang lưu trú thì chặn đăng ký và bắt đầu; ai làm lệnh nào theo bảng
 * FR-045, Quản lý viện xác nhận thay khi tầng không có Trưởng tầng được giao.
 */
class FR_040_StayLimitIT extends StayTestBase {

    @Test
    void Q30_overlapWithConfirmedStayOfSameResident_isRejected() throws Exception {
        confirmed(rMember, at(day, 18, 0), at(day.plusDays(3), 8, 0));

        register(office, mMember, at(day.plusDays(1), 18, 0), at(day.plusDays(2), 8, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FAMILY_STAY_OVERLAP"));
        register(office, mMember, at(day.plusDays(3), 9, 0), at(day.plusDays(4), 8, 0))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_040_pendingStaysDoNotCount_butConfirmingSecondOverlapIsRejected() throws Exception {
        long first = registered(rMember, at(day, 18, 0), at(day.plusDays(2), 8, 0));
        long second = registered(mMember, at(day.plusDays(1), 18, 0), at(day.plusDays(2), 8, 0));

        command(staff(floorManager), first, "confirm", "{}").andExpect(status().isOk());
        command(staff(floorManager), second, "confirm", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FAMILY_STAY_OVERLAP"));
        assertThat(stayStatus(second)).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void FR_040_sameFamilyMember_cannotHaveTwoOpenOverlappingStays() throws Exception {
        long other = resident(ResidentStatus.IN_RESIDENCE);
        fixtures.relate(rMember, other, 1, true, true);
        registered(rMember, at(day, 18, 0), at(day.plusDays(2), 8, 0));

        postAs(
                        office,
                        "/api/v1/family-stays",
                        registration(other, rMember, at(day.plusDays(1), 18, 0), at(day.plusDays(3), 8, 0), false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FAMILY_STAY_OVERLAP"));
    }

    @Test
    void FR_040_familyMemberWithoutActiveRelationship_isRejected() throws Exception {
        long stranger = fixtures.familyMember("Ngô S", "0906000009");

        register(office, stranger, at(day, 18, 0), at(day.plusDays(1), 8, 0))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void US8_4_isolation_blocksRegistrationAndStart() throws Exception {
        long id = confirmed(rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0));
        isolation.resident(a, List.of(new IsolationZoneView(902, Set.of(), Set.of(), time.now(), null)));

        register(office, mMember, at(day.plusDays(5), 18, 0), at(day.plusDays(6), 8, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ISOLATION_ACTIVE"));
        moveTo(at(day, 18, 0));
        command(office, id, "start", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ISOLATION_ACTIVE"));
        assertThat(stayStatus(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_041_residentNotInResidence_blocksRegistration() throws Exception {
        jdbc.update("UPDATE resident SET status = 'TEMPORARILY_ABSENT' WHERE id = ?", a);

        register(office, rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_NOT_PRESENT"));
    }

    @Test
    void FR_045_onlyOfficeRegisters_onlyAssignedFloorManagerConfirms() throws Exception {
        register(staff(floorManager), rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0))
                .andExpect(status().isForbidden());
        register(family(rMember), rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0))
                .andExpect(status().isForbidden());
        long id = registered(rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0));

        command(office, id, "confirm", "{}").andExpect(status().isForbidden());
        command(staff(manager), id, "confirm", "{}").andExpect(status().isForbidden());
        command(staff(users.staff(Role.FLOOR_MANAGER)), id, "confirm", "{}").andExpect(status().is4xxClientError());
        assertThat(stayStatus(id)).isEqualTo("PENDING_CONFIRMATION");
    }

    /** Màn web đọc danh sách không kèm người cao tuổi: Trưởng tầng (phạm vi được giao) thấy lượt của tầng mình. */
    @Test
    void FR_045_floorManagerListsStaysInScope_withoutResidentFilter() throws Exception {
        long id = registered(rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0));

        assertThat(getJsonAs(staff(floorManager), "/api/v1/family-stays").findValues("id"))
                .anySatisfy(v -> assertThat(v.asLong()).isEqualTo(id));
    }

    @Test
    void FR_045_floorWithoutFloorManager_managerConfirms() throws Exception {
        long other = resident(ResidentStatus.IN_RESIDENCE);
        long bareFloor = TestUsers.nextId();
        long bareRoom = TestUsers.nextId();
        locations
                .floor(bareFloor, null)
                .room(bareRoom, bareFloor)
                .stay(other, bareRoom, time.now().minus(java.time.Duration.ofDays(30)), null);
        fixtures.relate(mMember, other, 1, true, true);
        long id = id(postAs(
                        office,
                        "/api/v1/family-stays",
                        registration(other, mMember, at(day, 18, 0), at(day.plusDays(1), 8, 0), false))
                .andExpect(status().isCreated()));

        command(staff(floorManager), id, "confirm", "{}").andExpect(status().is4xxClientError());
        command(staff(manager), id, "confirm", "{}").andExpect(status().isOk());
        assertThat(stayStatus(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_045_cancelAndExtend_byOfficeWithReason() throws Exception {
        long pending = registered(rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0));
        command(office, pending, "cancel", "{}").andExpect(status().isUnprocessableContent());
        command(staff(floorManager), pending, "cancel", "{\"reason\": \"Gia đình đổi ý\"}")
                .andExpect(status().isForbidden());
        command(office, pending, "cancel", "{\"reason\": \"Gia đình đổi ý\"}").andExpect(status().isOk());
        assertThat(stayStatus(pending)).isEqualTo("CANCELLED");

        long id = confirmed(mMember, at(day, 18, 0), at(day.plusDays(1), 8, 0));
        moveTo(at(day, 18, 0));
        command(office, id, "start", "{}").andExpect(status().isOk());
        String newEnd = at(day.plusDays(2), 8, 0).toString();
        command(office, id, "extend", "{\"newExpectedEndAt\": \"" + newEnd + "\"}")
                .andExpect(status().isUnprocessableContent());
        command(office, id, "extend", "{\"newExpectedEndAt\": \"" + newEnd + "\", \"reason\": \"Chưa xuất viện\"}")
                .andExpect(status().isOk());

        assertThat(stayStatus(id)).isEqualTo("STAYING");
        assertThat(getJson("/api/v1/family-stays?residentId=" + a).findValues("expectedEndAt"))
                .anySatisfy(v ->
                        assertThat(java.time.Instant.parse(v.asString())).isEqualTo(java.time.Instant.parse(newEnd)));
    }
}
