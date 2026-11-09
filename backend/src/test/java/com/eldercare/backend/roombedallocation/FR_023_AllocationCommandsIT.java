package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
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
}
