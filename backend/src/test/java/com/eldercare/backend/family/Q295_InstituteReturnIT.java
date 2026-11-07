package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.CheckoutType;
import com.eldercare.backend.care.api.spi.DayCarePickupPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;

/**
 * 012 T018 (US2 kịch bản 12; FR-021, FR-027; Q-295; Q20): "Viện đưa về" ghi bản ghi đón và điểm danh về của 005 trong
 * cùng một lệnh tại nơi bàn giao, nên bản ghi Đã dùng ngay và không có hạn CFG-M10-09; 005 lỗi thì không còn bản ghi đón;
 * người ở nhà ngoài danh sách thì bị chặn như đón tại viện. Điểm danh về của 005 thay bằng bản giả gọi lại port của 012
 * như 005 làm ({@code valid}, {@code markUsed}).
 */
class Q295_InstituteReturnIT extends PickupTestBase {

    @MockitoSpyBean
    DayCareAttendanceApi attendance;

    @Autowired
    DayCarePickupPort dayCarePickups;

    @Test
    void US2_12_instituteReturn_checksOutInTheSameCommand_recordIsUsedAtOnce() throws Exception {
        AtomicBoolean inTransaction = new AtomicBoolean();
        stubbing(() -> doAnswer(call -> {
                    inTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
                    long recordId = call.getArgument(2);
                    Instant at = call.getArgument(4);
                    assertThat(dayCarePickups.valid(recordId, a, at))
                            .hasValueSatisfying(v -> assertThat(v.pickupType()).isEqualTo("INSTITUTE_RETURN"));
                    dayCarePickups.markUsed(recordId, 4242L);
                    return null;
                })
                .when(attendance)
                .checkOut(eq(a), any(LocalDate.class), anyLong(), eq(CheckoutType.INSTITUTE_RETURN), any(), any()));

        JsonNode record =
                json(pickup(office, "INSTITUTE_RETURN", mMember, M_DOC).andExpect(status().isCreated()));

        assertThat(inTransaction).isTrue();
        stubbing(() -> verify(attendance)
                .checkOut(
                        eq(a),
                        eq(LocalDate.ofInstant(Instant.parse(record.get("at").asString()), BusinessTime.ZONE)),
                        eq(record.get("id").asLong()),
                        eq(CheckoutType.INSTITUTE_RETURN),
                        eq(Instant.parse(record.get("at").asString())),
                        any(CurrentUser.class)));
        assertThat(record.get("status").asString()).isEqualTo("USED");
        assertThat(record.has("expiresAt") && !record.get("expiresAt").isNull()).isFalse();
    }

    @Test
    void Q20_careRefusesTheCheckout_noPickupRecordRemains() throws Exception {
        stubbing(() -> doThrow(new InvalidValueException("Người bán trú chưa có mặt"))
                .when(attendance)
                .checkOut(eq(a), any(LocalDate.class), anyLong(), any(), any(), any()));

        pickup(office, "INSTITUTE_RETURN", mMember, M_DOC).andExpect(status().is4xxClientError());

        assertThat(pickupRecords()).isZero();
    }

    @Test
    void US2_12_neighbourNotOnTheList_isBlocked_noCheckout() throws Exception {
        pickup(office, "INSTITUTE_RETURN", xMember, X_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.blockReason").value("NOT_ON_LIST"));

        stubbing(() -> verify(attendance, never()).checkOut(anyLong(), any(), anyLong(), any(), any(), any()));
        assertThat(lastBlockReason()).isEqualTo("NOT_ON_LIST");
    }

    /** {@code checkOut} là {@code MANDATORY}: spy nằm sau proxy giao dịch nên stub, verify cũng chạy trong transaction. */
    private void stubbing(Runnable call) {
        tx.executeWithoutResult(s -> call.run());
    }

    @Test
    void FR_022_floorManagerHasNoDayCareCheckout() throws Exception {
        pickup(staffSession(Role.FLOOR_MANAGER), "INSTITUTE_RETURN", mMember, M_DOC)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOURCE_COMMAND_PERMISSION_DENIED"));
    }
}
