package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.events.*;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T035: nhận đúng sự kiện M01, đặt/gỡ dấu; không hủy phân bổ tương lai. */
class FR_034_AllocationSuitabilityIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    FakeAllocationResidencySource residency;

    @Test
    void FR_034_m01EventsReassessFutureAllocationAndRemoveFlagWhenCompatibleAgain() {
        assertThat(context.containsBean("allocationSuitabilityListener"))
                .as("Cần listener sự kiện M01 của T035")
                .isTrue();
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'SPECIAL',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CONTRACT')",
                resident,
                bed,
                Timestamp.from(time.now().plusSeconds(3600)));
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        publish(new InitialAssessmentConfirmed(resident, 1, CareLevel.SPECIAL));
        flag(resident, true);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("FUTURE");
        jdbc.update("UPDATE resident SET current_care_level='BASIC' WHERE id=?", resident);
        publish(new CareLevelChanged(
                resident,
                Optional.of(CareLevel.SPECIAL),
                CareLevel.BASIC,
                time.now(),
                CareLevelBasisType.ASSESSMENT,
                1));
        flag(resident, false);
        jdbc.update("UPDATE room SET gender_policy='MALE' WHERE id=?", room);
        publish(new PersonalInfoUpdated(resident, Set.of("gender")));
        flag(resident, true);
    }

    private void publish(Object event) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event));
    }

    private void flag(long resident, boolean expected) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT room_unsuitable FROM bed_allocation WHERE resident_id=?",
                                Boolean.class,
                                resident))
                        .isEqualTo(expected));
    }
}
