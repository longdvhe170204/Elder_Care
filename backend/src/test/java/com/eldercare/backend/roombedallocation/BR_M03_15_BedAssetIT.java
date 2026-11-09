package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * 019 T019 (US5; FR-015, FR-016, BR-M03-15, Q-232, Q-235, Q-242, Q-245, Q-246, Q-256, Q-265; 003 FR-013, FR-013a,
 * FR-013b, Q-51): trạng thái tài sản giường đi cùng trạng thái giường qua cổng của 003, cùng transaction. Bản cài thật
 * của {@link BedAssetPort} (019) và của cổng lệnh giường (003); chỉ nguồn cách ly của 007 là bản giả. Tài sản giường
 * chèn thẳng vì ghi tăng kèm lệnh Tạo giường chờ chủ dự án chốt bộ phận Chăm sóc (trả 503).
 */
@Import(BR_M03_15_BedAssetIT.Sources.class)
class BR_M03_15_BedAssetIT extends AssetTestBase {

    @TestConfiguration
    static class Sources {
        @Bean
        @Primary
        FakeAllocationInfectionStatus bedAssetInfection() {
            return new FakeAllocationInfectionStatus();
        }
    }

    @Autowired
    BedAssetPort port;

    @Autowired
    com.eldercare.backend.roombedallocation.internal.bed.service.BedService bedService;

    @Autowired
    ApprovalRequestApi approvals;

    @Autowired
    List<RequestTypeHandler> handlers;

    long bed, asset;

    @BeforeEach
    void bedFixture() {
        bed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G5')", bed, room);
        asset = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,bed_id,status,location_type,location_id,"
                        + "floor_area_id,created_at) VALUES(?,?,?,?,?,'BED',?,'READY','ROOM',?,?,?)",
                asset,
                "BED-" + bed,
                bedType,
                "Giường G5",
                care,
                bed,
                room,
                floor,
                LocalDateTime.now());
        as(floorManager);
    }

    String bedState() {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed);
    }

    boolean flagged() {
        return jdbc.queryForObject("SELECT awaiting_resident_transfer FROM asset WHERE id=?", Boolean.class, asset);
    }

    long occupy() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,"
                        + "created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                java.sql.Timestamp.from(time.now()),
                java.sql.Timestamp.from(time.now()),
                manager.user().accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,expected_end,checked_care_level,"
                        + "care_level_source) VALUES(?,?,'ACTIVE','2020-01-01','2030-01-01','BASIC','CURRENT')",
                resident,
                bed);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        return resident;
    }

    void release(long resident) {
        jdbc.update(
                "UPDATE bed_allocation SET status='CLOSED',end_at='2020-02-01' WHERE resident_id=? AND bed_id=?",
                resident,
                bed);
        jdbc.update("UPDATE bed SET state='EMPTY' WHERE id=?", bed);
        history("OCCUPIED", "EMPTY", "TRANSFER_OUT", time.now().minus(java.time.Duration.ofHours(1)));
    }

    void history(String from, String to, String event, java.time.Instant at) {
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,from_state,to_state,event,occurred_at,basis_type,basis_id,"
                        + "floor_area_id) VALUES(?,?,?,?,?,'TEST',1,?)",
                bed,
                from,
                to,
                event,
                java.sql.Timestamp.from(at),
                floor);
    }

    long reminders(String event) {
        return events.stream(com.eldercare.backend.roombedallocation.internal.asset.domain.AssetReminder.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.eventType().equals(event)
                        && n.recipientGroups()
                                .contains(new RecipientGroup.FloorManager(floor, new Substitution.Default())))
                .filter(n -> n.sourceRecord().id().equals(Long.toString(asset)))
                .count();
    }

    @Autowired
    com.eldercare.backend.roombedallocation.internal.asset.service.AssetReminders assetReminders;

    @Autowired
    com.eldercare.backend.roombedallocation.api.BedStatusApi bedStatus;

    long future() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,'FUTURE','2099-01-01','BASIC','CURRENT')",
                resident,
                bed);
        return resident;
    }

    String allocationStatus(long resident) {
        return jdbc.queryForObject(
                "SELECT status FROM bed_allocation WHERE resident_id=? AND bed_id=?", String.class, resident, bed);
    }

    List<NotificationRequest> notices() {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .toList();
    }

    List<NotificationRequest> noticesTo(RecipientGroup group) {
        return notices().stream()
                .filter(n -> n.recipientGroups().contains(group))
                .toList();
    }

    @Test
    void US5_1_damageOnAnEmptyBedMovesTheBedToMaintenanceInTheSameCommand() throws Exception {
        command(asset, "report-damage", "{\"description\":\"Gãy thanh chắn\"}").andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("DAMAGED");
        assertThat(bedState()).isEqualTo("UNDER_MAINTENANCE");
    }

    @Autowired
    com.eldercare.backend.administration.shared.api.StatusHistoryApi statusHistory;

    org.springframework.test.web.servlet.ResultActions moveBed(long toRoom, long version) throws Exception {
        return mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/beds/" + bed)
                        .header("If-Match", com.eldercare.backend.common.web.VersionEtag.of(version))
                        .header("Idempotency-Key", "bed-move-" + TestUsers.nextId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + toRoom + ",\"code\":\"G5\"}"));
    }

    /** 003 T017 (FR-007, Q-243; 019 FR-012): giường chưa từng phân bổ đổi phòng, tài sản giường đổi vị trí theo. */
    @Test
    void Q_243_unallocatedBedMovesWithItsAssetAndAllocatedBedStaysBlocked() throws Exception {
        long newRoom = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) SELECT ?,?,room_type_id,?,"
                        + "'Phòng 105','UNRESTRICTED' FROM room WHERE id=?",
                newRoom,
                otherFloor,
                "P" + newRoom,
                room);
        // Tài sản giường thật luôn có khoảng vị trí mở ở phòng của giường (019 FR-012).
        statusHistory.add(
                com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds.LOCATION,
                asset,
                new com.eldercare.backend.administration.shared.api.dto.HistoryEntry<>(
                        com.eldercare.backend.administration.shared.api.dto.HistoryKind.PERIOD,
                        null,
                        "ROOM",
                        room,
                        time.now().minusSeconds(3600),
                        "CREATE",
                        null,
                        true,
                        null,
                        null,
                        null,
                        new com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds.Location(
                                "ROOM", room, null),
                        floor,
                        null));
        as(manager);
        moveBed(newRoom, 0).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT room_id FROM bed WHERE id=?", Long.class, bed))
                .isEqualTo(newRoom);
        assertThat(jdbc.queryForMap("SELECT location_type, location_id, floor_area_id FROM asset WHERE id=?", asset))
                .containsEntry("location_type", "ROOM")
                .containsEntry("location_id", newRoom)
                .containsEntry("floor_area_id", otherFloor);
        var moved = statusHistory.list(
                com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds.LOCATION, asset);
        assertThat(moved).hasSize(2);
        assertThat(moved.get(0).valueId()).as("phòng cũ").isEqualTo(room);
        assertThat(moved.get(1).valueId()).as("phòng mới").isEqualTo(newRoom);
        assertThat(moved.get(1).actorId()).isEqualTo(manager.staffId());
        assertThat(moved.get(1).floorAreaId()).isEqualTo(otherFloor);
        assertThat(moved.get(0).endsAt()).isNotNull().isEqualTo(moved.get(1).startsAt());
        assertThat(moved.get(1).endsAt()).isNull();

        long resident = occupy();
        release(resident);
        moveBed(room, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("FR-007"));
    }
}
