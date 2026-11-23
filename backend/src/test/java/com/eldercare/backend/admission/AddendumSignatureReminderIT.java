package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.AddendumSignatureOverdue;
import com.eldercare.backend.admission.api.events.AddendumSignatureReminder;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * FR-043d (Q-420, R7): phụ lục Chờ ký thì Hành chính được nhắc mỗi CFG-M02-17 (mặc định 1 ngày) từ ngày sau ngày duyệt;
 * từ ngày duyệt cộng CFG-M02-18 (mặc định 7 ngày) thì Quản lý viện được báo đúng một lần; ký xong thì thôi nhắc.
 */
@AutoConfigureMockMvc
@WithMockUser
class AddendumSignatureReminderIT extends ChangeTestBase {

    @Test
    void R7_remindedDaily_managerNotifiedOnce_stopsAfterSigning() throws Exception {
        Setup s = activeSetup();
        noonOf(today());
        LocalDate approvedOn = today();
        long extra = service("ACTIVITY", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = createApproveUnsigned(request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + extra + ", \"action\": \"ADD\"}]}",
                approvedOn.plusDays(30)));

        runDailyJob();
        assertThat(reminders(id)).as("ngày duyệt chưa nhắc").isZero();

        for (int d = 1; d <= 8; d++) {
            noonOf(approvedOn.plusDays(d));
            runDailyJob();
            runDailyJob(); // chạy lại cùng ngày không nhắc lặp
        }
        assertThat(reminders(id)).isEqualTo(8);
        assertThat(overdue(id)).as("báo Quản lý viện một lần từ ngày thứ 7").isEqualTo(1);

        sign(id, today()).andExpect(status().isOk());
        noonOf(approvedOn.plusDays(9));
        runDailyJob();
        assertThat(reminders(id)).isEqualTo(8);
    }

    private long reminders(long requestId) {
        return events.stream(AddendumSignatureReminder.class)
                .filter(e -> e.requestId() == requestId)
                .count();
    }

    private long overdue(long requestId) {
        return events.stream(AddendumSignatureOverdue.class)
                .filter(e -> e.requestId() == requestId)
                .count();
    }
}
