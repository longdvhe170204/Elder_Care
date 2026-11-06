package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.HoldCancellationReason;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 003 T033 lượt 1 (hợp đồng mục 1.4; FR-012, FR-014, FR-015, BR-M02-07): bản cài thật {@link BedAllocationCommandApi}
 * cho 004. Giữ chỗ hồ sơ chờ, hủy giữ chỗ theo mã lý do (idempotent), giải phóng giường khi vắng, gia hạn kết thúc dự
 * kiến; mọi hàm trong transaction của lệnh 004.
 */
class T033_BedAllocationCommandApiIT extends StructureTestSupport {
    @Autowired
    BedAllocationCommandApi commands;

    @Autowired
    TransactionTemplate tx;

    String state() {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed);
    }

    @Test
    void FR_014_waitingRecordHoldAndIdempotentCancellationByReasonCode() {
        long waiting = TestUsers.nextId(), resident = TestUsers.nextId();
        tx.executeWithoutResult(s -> commands.holdForWaitingRecord(waiting, resident, bed, time.now()));
        assertThat(state()).isEqualTo("HELD");
        tx.executeWithoutResult(s -> commands.cancelHold(waiting, new HoldCancellationReason("BED_DECLINED")));
        tx.executeWithoutResult(s -> commands.cancelHold(waiting, new HoldCancellationReason("BED_DECLINED")));
        assertThat(state()).isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject(
                        "SELECT reason FROM bed_state_history WHERE bed_id=? AND to_state='EMPTY' ORDER BY id DESC LIMIT 1",
                        String.class,
                        bed))
                .isEqualTo("BED_DECLINED");
    }

    @Test
    void BR_M02_07_releaseWhenAbsentClosesTheAllocation() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,'ACTIVE','2026-01-01','BASIC','CURRENT')",
                resident,
                bed);
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=? WHERE id=?",
                resident,
                bed);
        tx.executeWithoutResult(
                s -> commands.releaseWhenAbsent(resident, time.now(), new Basis("BR-M02-07", null, 1L, null)));
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM bed_allocation WHERE resident_id=? AND bed_id=?",
                        String.class,
                        resident,
                        bed))
                .isEqualTo("CLOSED");
        assertThat(state()).isEqualTo("AWAITING_READY");
    }

    @Test
    void FR_015_extendExpectedEndOfTheActiveAllocation() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,expected_end,checked_care_level,"
                        + "care_level_source) VALUES(?,?,'ACTIVE','2026-01-01','2026-02-01','BASIC','CURRENT')",
                resident,
                bed);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        var end = time.now().plus(Duration.ofDays(400));
        tx.executeWithoutResult(s -> commands.extendExpectedEnd(resident, end));
        assertThat(jdbc.queryForObject(
                        "SELECT expected_end IS NOT NULL AND expected_end > '2026-02-01' FROM bed_allocation"
                                + " WHERE resident_id=?",
                        Boolean.class,
                        resident))
                .isTrue();
    }

    @Test
    void T033_commandsNeedTheCallerTransaction() {
        assertThatThrownBy(() -> commands.holdForWaitingRecord(1L, 2L, bed, time.now()))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }
}
