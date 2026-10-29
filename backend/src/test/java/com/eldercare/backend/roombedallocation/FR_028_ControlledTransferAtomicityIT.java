package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

import com.eldercare.backend.common.*;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.isolation.service.ControlledTransferService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Transaction thật kết thúc trước assertion; chứng minh rollback khi lỗi sau cả hai lần chuyển trạng thái. */
class FR_028_ControlledTransferAtomicityIT extends StructureTestSupport {
    @Autowired
    ControlledTransferService transfers;

    @MockitoSpyBean
    FakeAllocationResidencySource source;

    long resident, allocation, target, doctor;

    @BeforeEach
    void fixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        doctor = new TestUsers(jdbc, time).staff(Role.DOCTOR).user().accountId();
        resident = TestUsers.nextId();
        allocation = TestUsers.nextId();
        target = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now().minusSeconds(60)),
                Timestamp.from(time.now().minusSeconds(60)),
                user.accountId());
        source.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'T2')", target, room);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT')",
                allocation,
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(60)));
    }

    @Test
    void FR_028_lateSourceFailureRollsBackAllocationsBedStatesHistoryAndAudit() {
        doThrow(new IllegalStateException("Nguồn004 lỗi sau đổi hai giường"))
                .when(org.springframework.test.util.AopTestUtils.<FakeAllocationResidencySource>getUltimateTargetObject(
                        source))
                .allocated(eq(resident), anyLong());
        assertThatThrownBy(() -> transfers.transfer(allocation, target, doctor, new Reason("Theo chỉ định")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_state_history WHERE bed_id IN (?,?)", Integer.class, bed, target))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE resident_id=? AND action='M03_INFECTION_CONTROL_TRANSFER'",
                        Integer.class,
                        resident))
                .isZero();
    }

    @Test
    void FR_008_adminCannotRewriteOrDeleteIsolationFacts() throws Exception {
        jdbc.update(
                "INSERT INTO room_isolation_history(room_id,action,occurred_at,actor_id,reason) VALUES(?,'ISOLATE',?,?,?)",
                room,
                Timestamp.from(time.now()),
                user.accountId(),
                "Chỉ định");
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE room_isolation_history SET reason='Sửa' WHERE room_id=" + room))
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("append-only");
            assertThatThrownBy(
                            () -> statement.executeUpdate("DELETE FROM room_isolation_history WHERE room_id=" + room))
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("retention");
        }
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO room_isolation_history(room_id,action,occurred_at,actor_id,reason) VALUES(?,'ISOLATE',?,?,?)",
                        room,
                        Timestamp.from(time.now()),
                        user.accountId(),
                        " "))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
