package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import com.eldercare.backend.roombedallocation.api.events.BedAllocationStarted;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 003 FR-018, FR-029; 004 FR-041, FR-045 (module-interfaces 003 mục 1.4a, chủ dự án duyệt 2026-11-17): người chưa có
 * giường (bán trú đổi sang nội trú) được kiểm và đặt trước phân bổ đầu tiên lúc duyệt yêu cầu; ngày hiệu lực phân bổ bắt
 * đầu, không đóng phân bổ nào. Loại lưu trú mới là tham số vì lúc duyệt người này còn là bán trú.
 */
@org.springframework.test.context.event.RecordApplicationEvents
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser
class FR_029_FirstAllocationReservationIT extends StructureTestSupport {
    @Autowired
    BedAllocationCommandApi commands;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ApplicationEvents events;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    FakeAllocationInfectionStatus infection;

    @Autowired
    FakeBedAssets assets;

    @Autowired
    com.eldercare.backend.roombedallocation.internal.allocation.service.ScheduledTransfers scheduled;

    long resident, request;
    LocalDate effective;

    @BeforeEach
    void dayCareResidentWithoutBed() {
        infection.reset();
        assets.reset();
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,"
                        + "created_at,created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.DAY_CARE, null, null, null, null);
        request = TestUsers.nextId();
        effective = time.today().plusDays(5);
    }

    List<UnmetCondition> conditions(Supplier<List<UnmetCondition>> check) {
        return tx.execute(s -> check.get());
    }

    List<String> codes(Supplier<List<UnmetCondition>> check) {
        return conditions(check).stream().map(UnmetCondition::code).toList();
    }

    String allocation(long id) {
        return jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id);
    }

    @Test
    void FR_029_firstAllocationIsReservedAtApprovalAndStartsOnTheEffectiveDate() {
        assertThat(codes(() -> commands.checkFirstAllocationReservation(
                        resident, bed, effective, null, ResidencyType.LONG_TERM)))
                .as("còn là bán trú lúc duyệt: xét theo loại lưu trú mới")
                .isEmpty();

        long id = tx.execute(
                s -> commands.reserveFirstAllocation(request, resident, bed, effective, null, ResidencyType.LONG_TERM));

        assertThat(jdbc.queryForMap(
                        "SELECT status,bed_id,basis_type,basis_id,previous_allocation_id FROM bed_allocation WHERE id=?",
                        id))
                .containsEntry("status", "FUTURE")
                .containsEntry("bed_id", bed)
                .containsEntry("basis_type", "APPROVAL_REQUEST")
                .containsEntry("basis_id", request)
                .containsEntry("previous_allocation_id", null);
        assertThat(jdbc.queryForObject("SELECT start_at FROM bed_allocation WHERE id=?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(effective.atStartOfDay(BusinessTime.ZONE).toInstant());
        assertThat(codes(() -> commands.checkScheduledTransfer(request)))
                .as("lúc kiểm áp dụng phiên bản hợp đồng mới chưa ghi: không xét FR-018 (b)")
                .isEmpty();
        assertThat(codes(() -> commands.checkFirstAllocationReservation(
                        resident, bed, effective, null, ResidencyType.LONG_TERM)))
                .contains("FUTURE_ALLOCATION_EXISTS");

        // Ngày hiệu lực: 004 đã ghi phiên bản nội dung mới (nội trú) rồi mới gọi 003
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        var result = tx.execute(s -> commands.applyScheduledTransfer(request, time.now()));

        assertThat(result).isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(allocation(id)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        assertThat(events.stream(BedAllocationStarted.class))
                .anyMatch(e -> e.allocationId() == id && e.residentId() == resident && e.bedId() == bed);
        assertThat(events.stream(BedTransferred.class)).noneMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_018_firstReservationIsRefusedWhenConditionsFailAndCancelledWithTheRequest() {
        jdbc.update("UPDATE room_type SET serves_short_term=FALSE WHERE id=?", type);
        assertThat(codes(() -> commands.checkFirstAllocationReservation(
                        resident, bed, effective, null, ResidencyType.SHORT_TERM)))
                .containsExactly("ALLOCATION_RESIDENCY_TYPE");
        assertThatThrownBy(() -> tx.execute(s -> commands.reserveFirstAllocation(
                        request, resident, bed, effective, null, ResidencyType.SHORT_TERM)))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .as("lỗi thì không ghi phân bổ")
                .isZero();

        long id = tx.execute(
                s -> commands.reserveFirstAllocation(request, resident, bed, effective, null, ResidencyType.LONG_TERM));
        tx.executeWithoutResult(s -> commands.cancelScheduledAllocation(request, new Reason("yêu cầu bị hủy")));
        assertThat(allocation(id)).isEqualTo("CANCELLED");

        // Người đã có phân bổ Hiệu lực thì phải dùng đổi giường
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,?,'ACTIVE','2026-01-01','BASIC','CURRENT')",
                TestUsers.nextId(),
                resident,
                bed);
        long other = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", other, room);
        assertThat(codes(() -> commands.checkFirstAllocationReservation(
                        resident, other, effective, null, ResidencyType.LONG_TERM)))
                .contains("ALLOCATION_ALREADY_ACTIVE");
    }

    /** Giường chưa Trống tới ngày hiệu lực: giữ Tương lai, chờ; giường về Trống thì bắt đầu, hoặc hủy khi FR-018 trượt. */
    @Test
    void FR_011a_firstAllocationWaitsForTheBedThenStartsOrIsCancelled() {
        long id = tx.execute(
                s -> commands.reserveFirstAllocation(request, resident, bed, effective, null, ResidencyType.LONG_TERM));
        awaitingReady(bed);
        assertThat(codes(() -> commands.checkScheduledTransfer(request)))
                .as("giường chưa Trống không phải vi phạm")
                .isEmpty();
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);

        assertThat(applyNow(request)).isEqualTo(ScheduledTransferResult.AWAITING_READY);
        assertThat(allocation(id)).isEqualTo("FUTURE");
        assertThat(jdbc.queryForObject(
                        "SELECT waiting_for_bed_ready FROM bed_allocation WHERE id=?", Boolean.class, id))
                .isTrue();

        empty(bed);
        assertThat(startWaiting(request)).isTrue();
        assertThat(allocation(id)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        assertThat(events.stream(BedAllocationStarted.class)).anyMatch(e -> e.allocationId() == id);

        // Người thứ hai: tới lúc giường về Trống vẫn là bán trú (phụ lục không áp dụng) thì FR-018 (b) trượt, 003 tự
        // hủy
        long second = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,"
                        + "created_at,created_by) VALUES(?,'[Họ tên NCT]','1951-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                second,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(second, ResidencyType.DAY_CARE, null, null, null, null);
        long secondBed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", secondBed, room);
        long secondRequest = TestUsers.nextId();
        long secondId = tx.execute(s -> commands.reserveFirstAllocation(
                secondRequest, second, secondBed, effective, null, ResidencyType.LONG_TERM));
        awaitingReady(secondBed);
        assertThat(applyNow(secondRequest)).isEqualTo(ScheduledTransferResult.AWAITING_READY);
        empty(secondBed);

        assertThat(startWaiting(secondRequest)).isFalse();
        assertThat(allocation(secondId)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, secondBed))
                .isEqualTo("EMPTY");
    }

    ScheduledTransferResult applyNow(long requestId) {
        return tx.execute(s -> commands.applyScheduledTransfer(requestId, time.now()));
    }

    boolean startWaiting(long requestId) {
        return tx.execute(s -> scheduled.startWaiting(requestId, time.now()));
    }

    void awaitingReady(long bedId) {
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(3600)),
                Timestamp.from(time.now().plusSeconds(10800)),
                bedId);
    }

    void empty(long bedId) {
        jdbc.update("UPDATE bed SET state='EMPTY',awaiting_ready_since=NULL,ready_deadline=NULL WHERE id=?", bedId);
    }
}
