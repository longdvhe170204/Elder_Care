package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T028 (US5, FR-048, BR-M08-11; quickstart Q33, Q34; tên {@code BR_M08_11_NhanPhieuIT} trong tasks): người nhận tại
 * tầng xác nhận Đã nhận một lần; người ngoài phạm vi tầng/khu 403 {@code OUT_OF_RECEIVING_SCOPE}; báo sai lệch cần nội
 * dung, phiếu Có sai lệch, bếp được báo; bếp ghi xử lý, phiếu về Đã giao, người báo được báo; nhận; lịch sử sai lệch giữ.
 */
class BR_M08_11_SlipReceiptIT extends SlipTestBase {

    long deliveredSlip() throws Exception {
        publishedWeek();
        residentIn(room2);
        residentIn(room3);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);
        clock.setTo(at(LocalTime.of(10, 30)));
        prepareAndDeliver(dailyMeal(lunch), slip, List.of());
        return slip;
    }

    @Test
    void BR_M08_11_receiverOfFloorReceivesOnce_outsiderIsOutOfScope() throws Exception {
        long slip = deliveredSlip();
        TestUser floor3Staff = receiverOf(floor3);
        TestUser floor2Staff = receiverOf(floor2);

        as(floor3Staff);
        slipCommand(slip, "receive", "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("OUT_OF_RECEIVING_SCOPE"));
        f.asKitchen();
        slipCommand(slip, "receive", "{}").andExpect(status().isForbidden());

        as(floor2Staff);
        slipCommand(slip, "receive", "{}").andExpect(status().isOk());
        slipCommand(slip, "receive", "{}").andExpect(status().isConflict());
        assertThat(slipStatus(slip)).isEqualTo("RECEIVED");
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(content->>'$.receivedBy' AS UNSIGNED) FROM simple_record WHERE id = ?",
                        Long.class,
                        slip))
                .isEqualTo(floor2Staff.accountId());
    }
}
