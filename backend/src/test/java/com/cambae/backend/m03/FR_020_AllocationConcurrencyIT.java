package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m03.api.spi.ResidencyType;
import com.cambae.backend.m03.internal.allocation.service.AllocationCommands;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** T020/SC-001: mười transaction thật cạnh tranh cùng giường. */
class FR_020_AllocationConcurrencyIT extends StructureTestSupport {
    @Autowired
    AllocationCommands commands;

    @Autowired
    FakeAllocationResidencySource residency;

    @Test
    void FR_020_tenConcurrentReservationsHaveExactlyOneWinner() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        List<Long> residents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long id = TestUsers.nextId();
            residents.add(id);
            jdbc.update(
                    "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                    id,
                    Timestamp.from(time.now()),
                    Timestamp.from(time.now()),
                    user.accountId());
            residency.resident(id, ResidencyType.LONG_TERM, null, null, null, null);
        }
        var ready = new CountDownLatch(10);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(10)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (long id : residents)
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Chưa mở cổng đồng thời");
                    try {
                        commands.create(id, bed, time.now().plusSeconds(3600), Reason.of("Phân bổ đồng thời"));
                        return true;
                    } catch (BusinessException error) {
                        assertThat(error.errorCode()).isIn("UNMET_CONDITIONS", "ALLOCATION_TIME_OVERLAP");
                        return false;
                    }
                }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_allocation WHERE bed_id=?", Integer.class, bed))
                .isEqualTo(1);
    }
}
