package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.service.BedTransferService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** T029/T032: transaction thật, không có transaction bao test để che lỗi hoàn tác. */
@RecordApplicationEvents
class BR_M03_07_BedTransferIT extends StructureTestSupport {
    @Autowired
    BedTransferService transfers;

    @Autowired
    ApplicationEvents published;

    @MockitoSpyBean
    FakeAllocationResidencySource residency;

    @Autowired
    FakeAllocationInfectionStatus infection;

    long resident, allocation, target, targetRoom, targetFloor, transferReason;

    @BeforeEach
    void transferFixture() {
        infection.reset();
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        allocation = TestUsers.nextId();
        target = TestUsers.nextId();
        targetRoom = TestUsers.nextId();
        targetFloor = TestUsers.nextId();
        transferReason = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now().minusSeconds(60)),
                Timestamp.from(time.now().minusSeconds(60)),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                targetFloor,
                area,
                "F" + targetFloor,
                "Tầng đích");
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                targetRoom,
                targetFloor,
                type,
                "R" + targetRoom,
                "Phòng đích");
        jdbc.update("UPDATE room SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", targetRoom);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'T1')", target, targetRoom);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT')",
                allocation,
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(60)));
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,code,name,status,attributes) VALUES(?,'M03.BED_TRANSFER_REASON','M03',?,?,'ACTIVE',JSON_OBJECT('medicalSafety',FALSE,'fixed',FALSE))",
                transferReason,
                "TR" + transferReason,
                "Theo nhu cầu " + transferReason);
    }

    private long transfer(long targetId, long reasonId, Reason reason) {
        return transfers.transfer(allocation, targetId, reasonId, reason);
    }

    @Test
    void BR_M03_07_closesAndOpensAtSameInstantWithMetadataBedStatesAuditAndEvent() throws Exception {
        long next = transfer(target, transferReason, new Reason("Chuyển tầng"));
        var previous = jdbc.queryForMap("SELECT status,end_at,end_reason FROM bed_allocation WHERE id=?", allocation);
        var current = jdbc.queryForMap(
                "SELECT status,start_at,previous_allocation_id,transfer_reason_id FROM bed_allocation WHERE id=?",
                next);
        assertThat(previous).containsEntry("status", "CLOSED").containsEntry("end_reason", "BED_TRANSFER");
        assertThat(current)
                .containsEntry("status", "ACTIVE")
                .containsEntry("previous_allocation_id", allocation)
                .containsEntry("transfer_reason_id", transferReason);
        assertThat(previous.get("end_at")).isEqualTo(current.get("start_at"));
        assertThat(state(bed)).isEqualTo("AWAITING_READY");
        assertThat(state(target)).isEqualTo("OCCUPIED");
        var events = published.stream(BedTransferred.class)
                .filter(e -> e.residentId() == resident)
                .toList();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().previousAllocationId()).isEqualTo(allocation);
        assertThat(events.getFirst().nextAllocationId()).isEqualTo(next);
        assertThat(events.getFirst().previousFloorId()).isEqualTo(floor);
        assertThat(events.getFirst().nextFloorId()).isEqualTo(targetFloor);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='M03_BED_TRANSFER' AND resident_id=? AND reason='Chuyển tầng'",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M03.ALLOCATION_REMINDER.AWAITING_REQUEST' AND entity_type='BED_ALLOCATION' AND entity_id=?",
                        Integer.class,
                        next))
                .isZero();
    }

    private String state(long id) {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id);
    }
}
