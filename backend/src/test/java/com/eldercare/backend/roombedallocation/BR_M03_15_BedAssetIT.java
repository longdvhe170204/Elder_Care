package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.api.events.BedBecameEmpty;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
    }

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

    @Test
    void US5_2_damageOnAnOccupiedBedKeepsStatesFlagsAndReReportingMovesToMaintenance() throws Exception {
        long resident = occupy();
        command(asset, "report-damage", "{\"description\":\"Lỏng khung\"}").andExpect(status().isUnprocessableEntity());
        command(asset, "report-damage", "{\"description\":\"Lỏng khung\",\"impactLevel\":\"STILL_SAFE\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("READY");
        assertThat(bedState()).isEqualTo("OCCUPIED");
        assertThat(flagged()).isTrue();
        assertThat(port.awaitingResidentTransfer(bed)).isTrue();
        assertThat(port.flaggedBeds(List.of(bed, bed + 1))).containsExactly(bed);
        assertThat(jdbc.queryForMap(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.impactLevel')) AS impact,"
                                + " JSON_EXTRACT(content,'$.awaitingTransfer') AS awaiting"
                                + " FROM record_event WHERE kind='M03.ASSET_DAMAGE' AND entity_type='M03.ASSET' AND entity_id=?",
                        asset))
                .containsEntry("impact", "STILL_SAFE")
                .containsEntry("awaiting", "true");
        assertThat(noticesTo(new RecipientGroup.FloorManager(floor, new Substitution.Default())))
                .as("nhắc Trưởng tầng chuyển người")
                .isNotEmpty();
        command(asset, "report-damage", "{\"description\":\"Lỏng khung\",\"impactLevel\":\"STILL_SAFE\"}")
                .andExpect(status().isConflict());

        release(resident);
        assertThat(bedState()).as("không tự đổi khi giường trống").isEqualTo("EMPTY");
        command(asset, "report-damage", "{\"description\":\"Báo hỏng lại\"}").andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("DAMAGED");
        assertThat(bedState()).isEqualTo("UNDER_MAINTENANCE");
        assertThat(flagged()).isFalse();
    }

    @Test
    void US5_3_passingMaintenanceReturnsTheBedToEmptyAndTriggersBrM0201() throws Exception {
        command(asset, "report-damage", "{\"description\":\"Gãy thanh chắn\"}").andExpect(status().isOk());
        command(asset, "start-maintenance", "{}").andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("UNDER_MAINTENANCE");
        assertThat(bedState()).isEqualTo("UNDER_MAINTENANCE");
        as(careLead);
        command(
                        asset,
                        "record-maintenance-result",
                        "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Thay thanh chắn\","
                                + "\"performedBy\":\"Tổ bảo trì\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("READY");
        assertThat(bedState()).isEqualTo("EMPTY");
        assertThat(events.stream(BedBecameEmpty.class).filter(e -> e.bedId() == bed))
                .hasSize(1);
    }

    @Test
    void US5_6_unsafeDamageSendsUrgentAndOnTheSpotRepairRemovesTheFlag() throws Exception {
        occupy();
        command(asset, "report-damage", "{\"description\":\"Gãy phanh\",\"impactLevel\":\"UNSAFE\"}")
                .andExpect(status().isOk());
        var urgent = notices().stream()
                .filter(n -> n.level().equals(java.util.Optional.of(NotificationLevel.URGENT)))
                .toList();
        assertThat(urgent).hasSize(1);
        assertThat(urgent.getFirst().recipientGroups())
                .as("Q-256: người đang nằm không có giường Trống đạt FR-018 ở tầng")
                .containsExactly(
                        new RecipientGroup.FloorManager(floor, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(floor, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        assertThat(urgent.getFirst().content().toString()).contains("chuyển người gấp", "tầng khác");

        as(medicalLead);
        command(asset, "remove-transfer-flag", "{\"reason\":\"Đã sửa tại chỗ\"}")
                .andExpect(status().isForbidden());
        as(floorManager);
        command(asset, "remove-transfer-flag", "{\"reason\":\"Đã sửa tại chỗ\"}")
                .andExpect(status().isUnprocessableEntity());
        command(
                        asset,
                        "remove-transfer-flag",
                        "{\"reason\":\"Đã sửa tại chỗ\",\"repairDescription\":\"Thay phanh\",\"repairedBy\":\"Tổ bảo trì\"}")
                .andExpect(status().isOk());
        assertThat(flagged()).isFalse();
        assertThat(stateOf(asset)).isEqualTo("READY");
        assertThat(bedState()).isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForMap(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.repairDescription')) AS repair,"
                                + " confirmed_at IS NOT NULL AS closed FROM record_event"
                                + " WHERE kind='M03.ASSET_DAMAGE' AND entity_type='M03.ASSET' AND entity_id=?",
                        asset))
                .containsEntry("repair", "Thay phanh")
                .containsEntry("closed", 1L);
        command(asset, "remove-transfer-flag", "{\"reason\":\"Lần nữa\"}").andExpect(status().isConflict());
    }

    @Test
    void FR_013b_removingTheFlagOnAnEmptyBedTriggersBrM0201AndManagerMayRemove() throws Exception {
        long resident = occupy();
        command(asset, "report-damage", "{\"description\":\"Lỏng khung\",\"impactLevel\":\"STILL_SAFE\"}")
                .andExpect(status().isOk());
        release(resident);
        as(otherFloorManager);
        command(asset, "remove-transfer-flag", "{\"reason\":\"Đã sửa tại chỗ\"}")
                .andExpect(status().isNotFound());
        as(manager);
        command(asset, "remove-transfer-flag", "{\"reason\":\"Đã sửa tại chỗ\"}")
                .andExpect(status().isOk());
        assertThat(flagged()).isFalse();
        assertThat(bedState()).isEqualTo("EMPTY");
        assertThat(events.stream(BedBecameEmpty.class).filter(e -> e.bedId() == bed))
                .hasSize(1);
    }

    @Test
    void FR_013b_flagKeepsTheFutureAllocationAndNotifiesOfficeAndFloor() throws Exception {
        occupy();
        long next = future();
        command(asset, "report-damage", "{\"description\":\"Lỏng khung\",\"impactLevel\":\"STILL_SAFE\"}")
                .andExpect(status().isOk());
        assertThat(allocationStatus(next)).isEqualTo("FUTURE");
        assertThat(noticesTo(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)))
                .as("báo Hành chính phân bổ tương lai bị ảnh hưởng")
                .isNotEmpty();
    }

    @Test
    void FR_015_occupiedBedBlocksMaintenanceAndDisposalLikeBrM0305() throws Exception {
        occupy();
        command(asset, "start-maintenance", "{}").andExpect(status().isConflict());
        as(careLead);
        command(asset, "request-disposal", "{\"reason\":\"Hỏng nặng\"}").andExpect(status().isConflict());
        assertThat(bedState()).isEqualTo("OCCUPIED");
        assertThat(stateOf(asset)).isEqualTo("READY");
    }

    @Test
    void Q_246_maintenanceOfASuspendedBedReturnsItToSuspended() throws Exception {
        as(manager);
        bedService.suspend(bed, com.eldercare.backend.common.Reason.of("Đóng tạm phòng"));
        assertThat(bedState()).isEqualTo("SUSPENDED");
        as(careLead);
        command(asset, "start-maintenance", "{}").andExpect(status().isOk());
        assertThat(bedState()).isEqualTo("UNDER_MAINTENANCE");
        command(
                        asset,
                        "record-maintenance-result",
                        "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Kiểm tra\",\"performedBy\":\"Tổ bảo trì\"}")
                .andExpect(status().isOk());
        assertThat(bedState()).isEqualTo("SUSPENDED");
        assertThat(events.stream(BedBecameEmpty.class).filter(e -> e.bedId() == bed))
                .isEmpty();
    }

    @Test
    void FR_016_bedPreparationDamageCancelsFutureAllocationsWhileOrdinaryDamageIsBlocked() throws Exception {
        jdbc.update("UPDATE bed SET state='AWAITING_READY',ready_deadline=? WHERE id=?", LocalDateTime.now(), bed);
        long next = future();
        command(asset, "report-damage", "{\"description\":\"Gãy thanh chắn\"}").andExpect(status().isConflict());
        assertThat(allocationStatus(next)).isEqualTo("FUTURE");
        as(otherFloorManager);
        command(asset, "link-bed-preparation-damage", "{\"description\":\"Gãy thanh chắn\"}")
                .andExpect(status().isNotFound());
        as(floorManager);
        command(asset, "link-bed-preparation-damage", "{\"description\":\"Gãy thanh chắn\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("DAMAGED");
        assertThat(bedState()).isEqualTo("UNDER_MAINTENANCE");
        assertThat(allocationStatus(next)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.source')) FROM record_event"
                                + " WHERE kind='M03.ASSET_DAMAGE' AND entity_type='M03.ASSET' AND entity_id=?",
                        String.class,
                        asset))
                .isEqualTo("BED_PREPARATION");
    }

    @Test
    void Q_51_approvedDisposalRetiresTheBedAndCancelsFutureAllocations() throws Exception {
        long next = future();
        as(careLead);
        command(asset, "request-disposal", "{\"reason\":\"Giường cũ\"}").andExpect(status().isOk());
        long request = jdbc.queryForObject("SELECT disposal_request_id FROM asset WHERE id=?", Long.class, asset);
        as(manager);
        String etag = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/approval-requests/" + request))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/approval-requests/" + request + "/approve")
                        .header("If-Match", etag)
                        .header("Idempotency-Key", "ar-" + TestUsers.nextId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        var view = approvals.find(request).orElseThrow();
        handlers.stream()
                .filter(h -> h.typeCode().equals("M03.ASSET_DISPOSAL"))
                .findFirst()
                .orElseThrow()
                .apply(view, view.actualEffectiveDate());
        assertThat(stateOf(asset)).isEqualTo("DISPOSED");
        assertThat(bedState()).isEqualTo("RETIRED");
        assertThat(allocationStatus(next)).isEqualTo("CANCELLED");
    }

    @Test
    void Q_242_bedAssetIsNotPutIntoUseAndBedCreationWaitsForTheCareDepartment() throws Exception {
        as(careLead);
        command(asset, "put-into-use", "{\"location\":{\"type\":\"ROOM\",\"id\":" + room + "}}")
                .andExpect(status().isConflict());
        assertThatThrownBy(() -> port.recordIncreaseForBed(bed + 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chăm sóc");
        assertThat(Set.of(stateOf(asset))).containsExactly("READY");
    }
}
