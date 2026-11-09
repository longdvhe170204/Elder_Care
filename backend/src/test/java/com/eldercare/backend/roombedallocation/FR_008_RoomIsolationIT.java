package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T053–T054: lệnh cách ly và ràng buộc nhật sử thật, không sửa trạng thái bằng endpoint danh mục. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
class FR_008_RoomIsolationIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Test
    void FR_008_managerIsolatesAndDoctorLiftsWithAppendOnlyHistory() throws Exception {
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{\"reason\":\"Phòng cần cách ly\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isolated").value(true));
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc.perform(post("/api/v1/rooms/" + room + "/lift-isolation")
                        .contentType("application/json")
                        .content("{\"reason\":\"Đủ điều kiện gỡ\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/rooms/" + room + "/isolation-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("ISOLATE"))
                .andExpect(jsonPath("$[1].action").value("LIFT"));
        assertThat(jdbc.queryForObject("SELECT isolated FROM room WHERE id=?", Boolean.class, room))
                .isFalse();
        assertThatThrownBy(() -> jdbc.update("UPDATE room_isolation_history SET reason='Sửa' WHERE room_id=?", room))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM room_isolation_history WHERE room_id=?", room))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Autowired
    com.eldercare.backend.roombedallocation.api.InfectionControlCommandApi infectionCommands;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus infection;

    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;

    private long roomOn(long floorId, boolean isolated) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy,isolated) VALUES(?,?,?,?,?,'UNRESTRICTED',?)",
                id,
                floorId,
                type,
                "R" + id,
                "Phòng " + id,
                isolated);
        return id;
    }

    private long flaggedAllocationIn(long roomId) {
        long bedId = TestUsers.nextId(), id = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,?,'OCCUPIED')", bedId, roomId, "G-" + bedId);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source,awaiting_doctor_confirmation,doctor_confirmation_requested_at) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT',TRUE,?)",
                id,
                TestUsers.nextId(),
                bedId,
                java.sql.Timestamp.from(time.now().minusSeconds(7200)),
                java.sql.Timestamp.from(time.now().minusSeconds(3600)));
        return id;
    }

    private boolean awaitingDoctor(long allocation) {
        return jdbc.queryForObject(
                "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE id=?", Boolean.class, allocation);
    }

    /** T053/T054 (FR-024a (c), duyệt 2026-11-09): 007 gọi API lệnh khi gỡ khoanh vùng. */
    @Test
    void FR_024a_cordonLiftedClearsDoctorFlagOnlyWhereTheRoomIsNoLongerBlocked() {
        infection.reset();
        long otherFloor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                otherFloor,
                area,
                "O" + otherFloor,
                "Tầng 2");
        long freed = flaggedAllocationIn(room);
        long isolatedRoom = flaggedAllocationIn(roomOn(floor, true));
        long stillCordoned = flaggedAllocationIn(roomOn(otherFloor, false));
        infection.cordon(
                time.now().minusSeconds(60),
                new com.eldercare.backend.roombedallocation.api.spi.CordonedArea(
                        java.util.Set.of(otherFloor), java.util.Set.of()));

        infectionCommands.cordonLifted(java.util.Set.of(floor, otherFloor), java.util.Set.of(), time.now());
        assertThat(awaitingDoctor(freed)).isFalse();
        assertThat(awaitingDoctor(isolatedRoom)).isTrue();
        assertThat(awaitingDoctor(stillCordoned)).isTrue();

        infectionCommands.cordonLifted(java.util.Set.of(floor, otherFloor), java.util.Set.of(), time.now());
        assertThat(awaitingDoctor(isolatedRoom)).isTrue();
        assertThat(awaitingDoctor(stillCordoned)).isTrue();
        infection.reset();
    }

    /** T054 (FR-045, US8 #5): người vừa rời giường còn Chờ sẵn sàng được thêm vào danh sách tiếp xúc → báo Trưởng tầng. */
    @Test
    void FR_045_contactListedNotifiesFloorManagerOnceForAnAwaitingReadyBed() {
        long departed = TestUsers.nextId(), closed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                closed,
                departed,
                bed,
                java.sql.Timestamp.from(time.now().minusSeconds(7200)),
                java.sql.Timestamp.from(time.now().minusSeconds(3600)));
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=?,last_closed_allocation_id=? WHERE id=?",
                java.sql.Timestamp.from(time.now().minusSeconds(3600)),
                java.sql.Timestamp.from(time.now().plusSeconds(3600)),
                closed,
                bed);

        infectionCommands.contactListed(departed, time.now());
        infectionCommands.contactListed(departed, time.now().plusSeconds(60));
        infectionCommands.contactListed(TestUsers.nextId(), time.now());

        var sent = events.stream(com.eldercare.backend.roombedallocation.api.events.FormerOccupantListed.class)
                .toList();
        assertThat(sent).hasSize(2).allSatisfy(e -> {
            assertThat(e.bedId()).isEqualTo(bed);
            assertThat(e.residentId()).isEqualTo(departed);
            assertThat(e.notificationRequests().getFirst().recipientGroups())
                    .containsExactly(new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                            floor, new com.eldercare.backend.common.notification.Substitution.Default()));
        });
        assertThat(sent.get(0).notificationRequests().getFirst().eventKey())
                .as("gọi lặp cùng lần rời giường: 009 chống trùng theo khóa")
                .isEqualTo(sent.get(1).notificationRequests().getFirst().eventKey());

        org.springframework.test.context.transaction.TestTransaction.end();
        assertThatThrownBy(() -> infectionCommands.contactListed(departed, time.now()))
                .as("MANDATORY: chỉ chạy trong transaction của lệnh 007")
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }
}
