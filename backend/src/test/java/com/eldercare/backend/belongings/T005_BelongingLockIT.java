package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.support.TestUsers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 013 T005 (research K-03, FR-011b): lệnh trên đồ của một người cao tuổi khóa dòng M12 của người đó rồi từng đồ theo
 * {@code id} tăng, so phiên bản mỗi đồ trong lệnh; lệch thì 409 {@code STATE_CHANGED} kèm trạng thái mới.
 */
class T005_BelongingLockIT extends BelongingsTestBase {

    @Autowired
    BelongingLocks locks;

    @Autowired
    TransactionTemplate tx;

    @Test
    void K_03_locksBelongingsInIdOrder_whenVersionsMatch() {
        long resident = TestUsers.nextId();
        long first = heldBelonging(resident, null);
        long second = heldBelonging(resident, null);
        Map<Long, Integer> versions = new LinkedHashMap<>();
        versions.put(second, 0);
        versions.put(first, 0);

        var locked = tx.execute(status -> locks.lock(resident, versions));

        assertThat(locked).extracting(LockedBelonging::id).containsExactly(first, second);
        assertThat(locked).extracting(LockedBelonging::status).containsOnly(BelongingStatus.HELD);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_module_state WHERE resident_id = ? AND module = 'M12'",
                        Integer.class,
                        resident))
                .isEqualTo(1);
    }

    @Test
    void FR_011b_staleVersion_isStateChanged_withCurrentState() {
        long resident = TestUsers.nextId();
        long belonging = heldBelonging(resident, null);
        jdbc.update(
                "UPDATE belonging SET version = 4, status = 'IN_USE', holder_type = 'RESIDENT', holder_id = ? WHERE id = ?",
                resident,
                belonging);

        assertThatThrownBy(() -> tx.execute(status -> locks.lock(resident, Map.of(belonging, 3))))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.errorCode()).isEqualTo("STATE_CHANGED");
                    assertThat(e.properties()).containsKey("currentValue");
                });
    }

    @Test
    void belongingOfAnotherResident_isNotFound() {
        long belonging = heldBelonging(TestUsers.nextId(), null);

        assertThatThrownBy(() -> tx.execute(status -> locks.lock(TestUsers.nextId(), Map.of(belonging, 0))))
                .isInstanceOf(NotFoundException.class);
    }
}
