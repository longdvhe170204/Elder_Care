package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationCommands;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** T025: thử lại toàn bộ transaction, không giữ lần ghi dở từ lần deadlock. */
class FR_020_AllocationRetryIT extends StructureTestSupport {
    @Autowired
    AllocationCommands commands;

    @Autowired
    FakeAllocationResidencySource residency;

    @MockitoSpyBean
    AllocationCommandStore store;

    @Test
    void FR_020_deadlockRetriesWithFreshTransactionAndOneAllocation() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        var attempts = new AtomicInteger();
        doAnswer(call -> {
                    var id = call.callRealMethod();
                    if (attempts.incrementAndGet() == 1)
                        throw new PessimisticLockingFailureException("Deadlock sau lần ghi đầu");
                    return id;
                })
                .when(store)
                .create(anyLong(), anyLong(), any(), any(), any(), anyString(), anyLong(), any());
        long id = commands.create(resident, bed, time.now().plusSeconds(3600), Reason.of("Bố trí giường"));
        assertThat(attempts).hasValue(2);
        assertThat(jdbc.queryForList("SELECT id FROM bed_allocation WHERE resident_id=?", Long.class, resident))
                .containsExactly(id);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M03.BED_ALLOCATION' AND entity_id=? AND action='M03_ALLOCATION_CREATED'",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(1);
    }
}
