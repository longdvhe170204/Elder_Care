package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryPeriod;
import com.eldercare.backend.belongings.internal.inventorycheck.service.InventoryCheckService;
import com.eldercare.backend.belongings.scheduling.M12DailyJob;
import com.eldercare.backend.belongings.scheduling.M12MinuteJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 013 T006 ({@code ChayBuM12IT}; research K-15, quickstart Q46, NFR-04): job M12 tắt qua mốc rồi chạy lại (lần chạy lúc
 * khởi động, rồi lần chạy theo lịch) xử lý việc đã đến hạn đúng một lần. Profile test tắt lịch và lần chạy lúc khởi
 * động nên test gọi {@code runNow()} thay cho hai lần đó.
 */
class M12JobCatchUpIT extends ReturnTestBase {

    @Autowired
    M12DailyJob dailyJob;

    @Autowired
    M12MinuteJob minuteJob;

    @Autowired
    InventoryCheckService checks;

    @Test
    void Q46_restartAfterShutdownAcrossThePeriodStart_remindsTheNewPeriodExactlyOnce() throws Exception {
        long resident = resident("IN_RESIDENCE");
        intake(resident, "PHONE", "\"quantity\": 1", location());
        InventoryPeriod before = checks.currentPeriod();

        // Tắt qua mốc đầu kỳ: đồng hồ sang ngày thứ hai của kỳ kế tiếp mà job không chạy
        for (int day = 0; day < 400 && checks.currentPeriod().code().equals(before.code()); day++) {
            clock.advanceBy(Duration.ofDays(1));
        }
        clock.advanceBy(Duration.ofDays(1));
        InventoryPeriod period = checks.currentPeriod();
        assertThat(period.code()).isNotEqualTo(before.code());
        forgetPeriodStart(period);

        dailyJob.runNow();
        dailyJob.runNow();

        assertThat(periodStartNotices(period)).isEqualTo(1);
    }

    @Test
    void Q46_minuteJobAfterShutdown_expiresTheConfirmationOnce_andNotifiesTheCreatorOnce() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long confirmation = id(postJson(
                        tokenA,
                        "/api/v1/recipient-confirmations",
                        "{\"residentId\": " + resident + ", \"recipientName\": \"Người nhận\","
                                + " \"recipientIdDocument\": \"999\", \"belongingIds\": [" + phone + "],"
                                + " \"signedForm\": {\"representativeId\": " + representative + ", \"fileId\": "
                                + photo() + "}}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(confirmationStatus(confirmation)).isEqualTo("EFFECTIVE");

        // Tắt quá hạn CFG-M10-08 (4 giờ) rồi chạy lại
        clock.advanceBy(Duration.ofDays(2));
        minuteJob.runNow();
        minuteJob.runNow();

        assertThat(confirmationStatus(confirmation)).isEqualTo("EXPIRED");
        assertThat(notices().stream()
                        .filter(n -> "RECIPIENT_CONFIRMATION_EXPIRED".equals(n.eventType())
                                && n.residentId().filter(id -> id == resident).isPresent())
                        .count())
                .isEqualTo(1);
    }

    /** Dấu đầu kỳ do lần chạy trước của schema test để lại (dữ liệu giữ qua các lần chạy); chỉ tài khoản quản trị xóa được. */
    private static void forgetPeriodStart(InventoryPeriod period) throws Exception {
        try (Connection connection = adminConnection();
                PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM reminder_marker WHERE kind = 'M12.INVENTORY_PERIOD_START' AND entity_id = ?")) {
            delete.setLong(1, period.start().toEpochDay());
            delete.executeUpdate();
        }
    }

    private long periodStartNotices(InventoryPeriod period) {
        List<String> keys = List.of("INVENTORY_PERIOD_STARTED:" + period.code());
        return notices().stream()
                .filter(n -> "INVENTORY_PERIOD_STARTED".equals(n.eventType()) && keys.contains(n.eventKey()))
                .count();
    }
}
