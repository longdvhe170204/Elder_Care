package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeInfectionControlCommands;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * 003 FR-045 (003 module-interfaces mục 1.10a, research G-08): người cao tuổi vào danh sách tiếp xúc thì lệnh 007 gọi
 * {@code InfectionControlCommandApi.contactListed} trong cùng transaction; 003 lỗi thì lệnh xác nhận lỗi theo và transaction
 * bị đánh dấu rollback. Dùng bản giả chung của 003 nạp ở {@link ContactTracingTestBase}. Lớp nền chạy mỗi test trong một
 * transaction ({@code @Transactional}), nên rollback kiểm bằng cờ rollback-only của transaction đó, không đọc lại bảng.
 */
class FR_045_ContactListedIT extends ContactTracingTestBase {
    @Autowired
    FakeInfectionControlCommands proxied;

    @Autowired
    PlatformTransactionManager transactions;

    FakeInfectionControlCommands infection;
    long listId;
    long contact;

    @BeforeEach
    void draftWithOneResident() throws Exception {
        // giao diện 003 có @Transactional nên bean giả bị bọc proxy: đọc danh sách lời gọi ở đối tượng gốc
        infection = AopTestUtils.getTargetObject(proxied);
        infection.reset();
        listId = createList(nurse);
        contact = TestUsers.nextId();
        var from = traceFrom.plus(Duration.ofDays(1));
        post(nurse, CONTACTS, listId, contact("RESIDENT", contact, "SAME_ROOM", from, discoveredAt, null))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_045_confirmingListTellsAllocationEachListedResident() throws Exception {
        post(nurse, CONFIRM, listId, "{}").andExpect(status().isOk());

        assertThat(infection.contactsListed)
                .singleElement()
                .satisfies(call -> assertThat(call.residentId()).isEqualTo(contact));
        assertThat(markedRollbackOnly()).isFalse();
    }

    @Test
    void FR_045_allocationFailureRollsBackConfirmation() throws Exception {
        infection.fail(new BusinessException(HttpStatus.CONFLICT, "ALLOCATION_FAILED", "003 lỗi"));

        post(nurse, CONFIRM, listId, "{}").andExpect(status().isConflict());

        assertThat(infection.contactsListed).isEmpty();
        assertThat(markedRollbackOnly()).isTrue();
    }

    /** Transaction đang mở (của test, lệnh 007 đã tham gia) có bị lệnh lỗi đánh dấu rollback không; gọi cuối test. */
    private boolean markedRollbackOnly() {
        var status = transactions.getTransaction(TransactionDefinition.withDefaults());
        try {
            return status.isRollbackOnly();
        } finally {
            transactions.rollback(status);
        }
    }
}
