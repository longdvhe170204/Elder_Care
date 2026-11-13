package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.spi.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/** T019/T025: cùng một kết quả FR-018 cho danh sách và lệnh, không dừng ở lỗi đầu. */
@Transactional
class BR_13_AllocationConditionsIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    jakarta.persistence.EntityManager entities;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    FakeAllocationInfectionStatus infection;

    @Autowired
    FakeBedAssets assets;

    long resident;

    @BeforeEach
    void residentFixture() {
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        infection.reset();
        assets.reset();
    }

    @SuppressWarnings("unchecked")
    private List<UnmetCondition> check(Instant start, Instant end, boolean future) throws Exception {
        assertThat(context.containsBean("allocationChecker"))
                .as("Cần checker FR-018 dùng chung")
                .isTrue();
        Object checker = context.getBean("allocationChecker");
        return (List<UnmetCondition>) checker.getClass()
                .getMethod("check", long.class, long.class, Instant.class, Instant.class, Long.class, boolean.class)
                .invoke(checker, resident, bed, start, end, null, future);
    }

    /** T053 (FR-009, research G-08): phòng không cách ly nhưng thuộc tầng đang khoanh vùng cũng chặn phân bổ. */
    @Test
    void BR_13_cordonedFloorBlocksAllocationLikeAnIsolatedRoom() throws Exception {
        infection.cordon(
                time.now().minusSeconds(60),
                new com.eldercare.backend.roombedallocation.api.spi.CordonedArea(
                        java.util.Set.of(floor), java.util.Set.of()));
        assertThat(check(time.now(), null, true))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_ISOLATION");
        infection.reset();
        assertThat(check(time.now(), null, true))
                .extracting(UnmetCondition::code)
                .doesNotContain("ALLOCATION_ISOLATION");
    }
}
