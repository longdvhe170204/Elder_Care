package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.service.BedTransferService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.List;
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

    @Test
    void FR_033_twoTransfersOnSameAllocationHaveOneWinner() throws Exception {
        long other = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'T2')", other, targetRoom);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (long next : new long[] {target, other})
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Chưa mở cổng đồng thời");
                    try {
                        transfer(next, transferReason, new Reason("Chuyển đồng thời"));
                        return true;
                    } catch (BusinessException error) {
                        assertThat(error.errorCode()).isEqualTo("ALLOCATION_NOT_TRANSFERABLE");
                        return false;
                    }
                }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int won = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) won++;
            assertThat(won).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=? AND status='ACTIVE'",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed WHERE id IN (?,?) AND state='OCCUPIED'",
                        Integer.class,
                        target,
                        other))
                .isEqualTo(1);
    }

    /**
     * FR-030, research G-07 (bỏ 503 theo bảng duyệt 16 điểm (3), 2026-11-09): phân bổ mang dấu chờ chuyển về phòng cùng giá
     * so loại phòng đích với đơn giá của hợp đồng hiện hành; khác giá mà lý do không thuộc y tế/an toàn thì 409, cùng giá thì
     * chuyển và phân bổ mới không còn dấu.
     */
    @Test
    void FR_030_flaggedAllocation_comparesWithContractPrice_sameContractPriceClearsFlag() throws Exception {
        jdbc.update("UPDATE bed_allocation SET awaiting_same_price=TRUE WHERE id=?", allocation);
        residency.contractPriceDifference(resident, type, time.today(), true);
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Về phòng cũ")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("RESIDENCY_CHANGE_REQUEST_REQUIRED"));
        unchanged();

        residency.contractPriceDifference(resident, type, time.today(), false);
        long next = transfer(target, transferReason, new Reason("Về phòng cùng giá"));

        assertThat(jdbc.queryForMap(
                        "SELECT status,awaiting_request,awaiting_same_price FROM bed_allocation WHERE id=?", next))
                .containsEntry("status", "ACTIVE")
                .containsEntry("awaiting_request", false)
                .containsEntry("awaiting_same_price", false);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("CLOSED");
    }

    private void differentPrice() {
        residency.priceDifference(resident, type, type, time.today(), true);
    }

    private String state(long id) {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id);
    }

    private void unchanged() {
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(1);
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        assertThat(state(target)).isEqualTo("EMPTY");
    }
}
