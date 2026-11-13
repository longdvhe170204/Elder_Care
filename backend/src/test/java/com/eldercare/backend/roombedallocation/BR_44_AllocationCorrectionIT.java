package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.*;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.events.BedAllocationCorrected;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.*;
import org.springframework.transaction.annotation.Transactional;

/** T047: dùng cơ chế đính chính thật, rollback toàn bộ khi FR-019 không thỏa. */
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
@RecordApplicationEvents
class BR_44_AllocationCorrectionIT extends StructureTestSupport {
    @Autowired
    CorrectionApi corrections;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.account.api.PermissionApi permissions;

    @Autowired
    List<CorrectableRecordProvider> providers;

    @Autowired
    ApplicationEvents events;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    long id, resident;

    @BeforeEach
    void correctionFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        id = allocate(resident, bed, start, start.plusSeconds(3600));
    }

    private long allocate(long who, long target, Instant from, Instant to) {
        long allocation = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source,opened_by_id) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT',?)",
                allocation,
                who,
                target,
                Timestamp.from(from),
                Timestamp.from(to),
                user.accountId());
        return allocation;
    }

    private CorrectableRecordProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.recordType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu provider " + type));
    }

    @Test
    void BR_44_amendChangesCurrentViewAndPublishesBeforeAfter() {
        assertThat(provider("M03.BED_ALLOCATION").floorArea(id)).contains(floor);
        var saved = corrections.create(
                "M03.BED_ALLOCATION",
                id,
                CorrectionType.AMEND_CONTENT,
                Map.of("endAt", start.plusSeconds(7200).toString()),
                new Reason("Sửa mốc kết thúc"));
        assertThat(saved).isInstanceOf(CorrectionResult.Saved.class);
        assertThat(jdbc.queryForObject("SELECT end_at FROM v_current_allocation WHERE id=?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(start.plusSeconds(7200));
        assertThat(events.stream(BedAllocationCorrected.class)).singleElement().satisfies(event -> {
            assertThat(event.allocationId()).isEqualTo(id);
            assertThat(event.before().get("endAt"))
                    .isEqualTo(start.plusSeconds(3600).toString());
            assertThat(event.after().get("endAt"))
                    .isEqualTo(start.plusSeconds(7200).toString());
        });
        assertThat(jdbc.queryForObject("SELECT end_at FROM bed_allocation WHERE id=?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(start.plusSeconds(3600));
    }
}
