package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.structure.domain.*;
import com.cambae.backend.m03.internal.structure.persistence.StructureStore;
import com.cambae.backend.support.TestUsers;
import java.sql.Timestamp;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** FR-006 phải kiểm dữ liệu mới sau chờ khóa, không bỏ sót người vừa được phân bổ. */
class FR_006_RoomPolicyConcurrencyIT extends StructureTestSupport {
    @Autowired
    PlatformTransactionManager transactions;

    @MockitoSpyBean
    StructureStore store;

    @Test
    void FR_006_seesAllocationCommittedWhileWaitingForBedLock() throws Exception {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        var policyAtBeds = new CountDownLatch(1);
        doAnswer(invocation -> {
                    policyAtBeds.countDown();
                    return invocation.callRealMethod();
                })
                .when(store)
                .lockRoomBeds(anyLong());
        try (var executor = Executors.newSingleThreadExecutor()) {
            @SuppressWarnings("unchecked")
            Future<Throwable>[] future = new Future[1];
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, bed);
                future[0] = executor.submit(() -> {
                    try {
                        structures.update(
                                StructureKind.ROOM,
                                room,
                                "\"0\"",
                                StructureChange.room(
                                        floor,
                                        type,
                                        "R" + room,
                                        "Phòng " + room,
                                        GenderPolicy.MALE,
                                        Set.of(CareLevelCode.BASIC)));
                        return null;
                    } catch (Throwable error) {
                        return error;
                    }
                });
                try {
                    assertThat(policyAtBeds.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                jdbc.update(
                        "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                        resident,
                        bed,
                        Timestamp.from(time.now()));
            });
            assertThat(future[0].get(15, TimeUnit.SECONDS)).isInstanceOf(ConditionNotMetException.class);
        }
        assertThat(jdbc.queryForObject("SELECT gender_policy FROM room WHERE id=?", String.class, room))
                .isEqualTo("UNRESTRICTED");
    }
}
