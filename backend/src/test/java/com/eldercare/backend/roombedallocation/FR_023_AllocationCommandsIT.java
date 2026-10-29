package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.api.spi.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/** T021/T026: trạng thái người quyết định loại phân bổ; nguồn và trạng thái giường cùng transaction. */
@Transactional
class FR_023_AllocationCommandsIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    FakeAllocationResidencySource residency;

    long resident;

    @BeforeEach
    void residentFixture() {
        user.set(new TestUsers(jdbc, time)
                .staff(com.eldercare.backend.common.Role.OFFICE_STAFF)
                .user());
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
    }

    private Object commands() {
        assertThat(context.containsBean("allocationCommands"))
                .as("Cần lệnh phân bổ thật")
                .isTrue();
        return context.getBean("allocationCommands");
    }

    private long create(Instant start) throws Exception {
        Object commands = commands();
        try {
            return (Long) commands.getClass()
                    .getMethod("create", long.class, long.class, Instant.class, Reason.class)
                    .invoke(commands, resident, bed, start, Reason.of("Bố trí giường"));
        } catch (InvocationTargetException error) {
            throw (Exception) error.getCause();
        }
    }

    @Test
    void FR_023_admissionCreatesFutureWithoutStartingBedAndNotifiesResidency() throws Exception {
        Instant start = time.now().plusSeconds(3600);
        long id = create(start);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("FUTURE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
        assertThat(residency.allocations()).containsEntry(resident, id);
    }

    @Test
    void BR_M03_19_inResidenceWithoutBedAllocatesImmediatelyAndClearsMarker() throws Exception {
        jdbc.update("UPDATE resident SET status='IN_RESIDENCE' WHERE id=?", resident);
        jdbc.update(
                "INSERT INTO resident_position(resident_id,without_bed_since) VALUES(?,?)",
                resident,
                Timestamp.from(time.now()));
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at) VALUES('RESIDENT',?,'WITHOUT_BED',?)",
                resident,
                Timestamp.from(time.now()));
        long id = create(time.now().plusSeconds(3600));
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject(
                        "SELECT without_bed_since FROM resident_position WHERE resident_id=?",
                        Timestamp.class,
                        resident))
                .isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_type='RESIDENT' AND target_id=? AND flag_type='WITHOUT_BED'",
                        Integer.class,
                        resident))
                .isZero();
    }

    @Test
    void FR_023_cannotCreateSecondFutureAllocation() throws Exception {
        create(time.now().plusSeconds(3600));
        assertThatThrownBy(() -> create(time.now().plusSeconds(7200))).isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_023_cancelsFutureButKeepsRecord() throws Exception {
        long id = create(time.now().plusSeconds(3600));
        Object commands = commands();
        commands.getClass()
                .getMethod("cancel", long.class, Reason.class)
                .invoke(commands, id, Reason.of("Đổi kế hoạch"));
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancellation_reason FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("Đổi kế hoạch");
    }

    @Test
    void FR_023_futureReservationKeepsTargetBedHeldForTheSameResident() throws Exception {
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='WAITING_RECORD',hold_resident_id=?,hold_waiting_record_id=?,hold_until=? WHERE id=?",
                resident,
                resident,
                Timestamp.from(time.now().plusSeconds(7200)),
                bed);
        create(time.now().plusSeconds(3600));
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT hold_resident_id FROM bed WHERE id=?", Long.class, bed))
                .isEqualTo(resident);
    }

    @Test
    void BR_M03_19_requiresWithoutBedMarkerForImmediateAllocation() {
        jdbc.update("UPDATE resident SET status='IN_RESIDENCE' WHERE id=?", resident);
        assertThatThrownBy(() -> create(null)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .isZero();
    }
}
