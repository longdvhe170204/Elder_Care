package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.events.BillableCareResultCorrected;
import com.eldercare.backend.care.api.events.BillableCareResultRecorded;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.*;

/**
 * FR-051, DBR-15, DBR-17 phần 005 (T033, T036, T050, T051), quyết định 05/11 ({@code specs/010-…/
 * care-quantity-decision-2026-11-05.md}): vật phẩm phát lượng thực đã ghi, dịch vụ theo lần phát 1; Không thực hiện
 * không phát; AMEND_CONTENT phát tổng mới kể cả khi không đổi, VOID {@code newQuantity = null}. Sự kiện ghi outbox cùng
 * transaction của lệnh. Loại kết quả mà đơn vị dịch vụ chưa xác định được (ml, mức ăn, vận động, đo…) vẫn bị chặn, không
 * quy đổi.
 */
@Transactional(isolation = Isolation.READ_COMMITTED)
class FR_051_BillableCareResultIT extends CareResultTestBase {

    private ResultActions correct(String body) throws Exception {
        return mvc.perform(post("/api/v1/care-results/" + task + "/correct")
                .header("Idempotency-Key", "correct-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long serviceId() {
        return jdbc.queryForObject(
                "SELECT CAST(JSON_EXTRACT(data,'$.serviceId') AS UNSIGNED) FROM care_task WHERE id=?",
                Long.class,
                task);
    }

    private BillableCareResultRecorded recorded() {
        var found = events.stream(BillableCareResultRecorded.class)
                .filter(e -> e.resultId() == task)
                .toList();
        assertThat(found).hasSize(1);
        return found.getFirst();
    }

    private int outbox(Class<?> type) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE=? AND JSON_EXTRACT(SERIALIZED_EVENT,'$.resultId')=?",
                Integer.class,
                type.getName(),
                task);
    }

    @Test
    void FR_051_itemQuantityPublishesRecordedAmountWithServiceInSameTransaction() throws Exception {
        data("ITEM_QUANTITY", true);
        record("{\"numericValue\":2}").andExpect(status().isOk());
        var event = recorded();
        assertThat(event.quantity()).isEqualByComparingTo("2");
        assertThat(event.residentId()).isEqualTo(resident);
        assertThat(event.taskTypeId()).isEqualTo(type);
        assertThat(event.serviceId()).isEqualTo(serviceId());
        assertThat(event.performedAt()).isNotNull();
        assertThat(storedStatus()).isEqualTo("COMPLETED");
        // Outbox của 000 ghi trong transaction của lệnh: còn thấy trước commit, mất khi rollback
        assertThat(outbox(BillableCareResultRecorded.class)).isEqualTo(1);
        TestTransaction.flagForRollback();
        TestTransaction.end();
        assertThat(outbox(BillableCareResultRecorded.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE id=?", Integer.class, task))
                .isZero();
    }

    @Test
    void FR_051_performedServicePublishesOneOccurrence() throws Exception {
        data("PERFORMED", true);
        record("{}").andExpect(status().isOk());
        assertThat(recorded().quantity()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void FR_051_notPerformedPublishesNothing() throws Exception {
        data("ITEM_QUANTITY", true);
        String name = "Lý do tính phí " + TestUsers.nextId();
        user.set(new TestUsers(jdbc, time)
                .staff(com.eldercare.backend.common.Role.NURSING_HOME_MANAGER)
                .user());
        mvc.perform(post("/api/v1/non-performance-reason-types")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());
        user.set(baseManager.user());
        long reason = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND name=?",
                Long.class,
                name);
        notPerform("{\"reasonTypeId\":" + reason + ",\"note\":\"Người cao tuổi từ chối\"}")
                .andExpect(status().isOk());
        assertThat(events.stream(BillableCareResultRecorded.class).filter(e -> e.resultId() == task))
                .isEmpty();
    }

    @Test
    void FR_051_unitNotDeterminableStaysBlockedWithoutEvent() throws Exception {
        data("ML", true);
        record("{\"numericValue\":200}").andExpect(status().isServiceUnavailable());
        assertThat(events.stream(BillableCareResultRecorded.class).filter(e -> e.resultId() == task))
                .isEmpty();
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void US7_4_amendPublishesNewTotalEvenWhenUnchanged_voidPublishesNullQuantity() throws Exception {
        data("ITEM_QUANTITY", true);
        record("{\"numericValue\":2}").andExpect(status().isOk());
        correct("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":3},\"reason\":\"Ghi nhầm số tã\"}")
                .andExpect(status().isCreated());
        correct("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":3,\"note\":\"Bổ sung ghi chú\"},"
                        + "\"reason\":\"Thêm ghi chú\"}")
                .andExpect(status().isCreated());
        correct("{\"type\":\"VOID\",\"reason\":\"Ghi nhầm người\"}").andExpect(status().isCreated());
        var corrected = events.stream(BillableCareResultCorrected.class)
                .filter(e -> e.resultId() == task)
                .toList();
        assertThat(corrected)
                .extracting(BillableCareResultCorrected::correctionType)
                .containsExactly("AMEND_CONTENT", "AMEND_CONTENT", "VOID");
        assertThat(corrected.get(0).newQuantity()).isEqualByComparingTo("3");
        assertThat(corrected.get(1).newQuantity()).isEqualByComparingTo("3");
        assertThat(corrected.get(2).newQuantity()).isNull();
        assertThat(corrected).extracting(BillableCareResultCorrected::eventKey).doesNotHaveDuplicates();
        // Chưa có consumer 010 (E/T021) nên outbox chưa ghi dòng cho sự kiện này; kiểm outbox khi E nối listener.
    }
}
