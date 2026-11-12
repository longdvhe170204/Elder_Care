package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.M02Reminder;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 004 T031, T036 (US3; FR-026, BR-M02-05, CFG-M02-04): hợp đồng dài hạn và bán trú được nhắc Hành chính mỗi ngày trong
 * CFG-M02-04 ngày trước ngày kết thúc (mặc định 30); hợp đồng ngắn ngày được nhắc đúng vào ngày kết thúc; hệ thống không
 * tự gia hạn hay tự kết thúc hợp đồng nào.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_05_ContractReminderIT extends ContractTestBase {

    private static final String EXPIRING = "CONTRACT_EXPIRING";
    private static final String SHORT_TERM = "SHORT_TERM_CONTRACT";

    @Autowired
    ApplicationEvents applicationEvents;

    /** Hợp đồng Hiệu lực kết thúc sau {@code days} ngày kể từ {@code from}. */
    private long contractEnding(ResidenceType type, LocalDate from, int days) {
        long id = contracts.contract(
                admissionResident(), ContractStatus.ACTIVE, type, CareLevel.BASIC, from.minusDays(200));
        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", from.plusDays(days), id);
        return id;
    }

    private List<M02Reminder> remindersOf(long contractId, String type) {
        return applicationEvents.stream(M02Reminder.class)
                .filter(reminder -> type.equals(reminder.type()) && reminder.subjectId() == contractId)
                .toList();
    }

    private void runOn(LocalDate day) {
        noonOf(day);
        runDailyJob();
    }

    @Test
    void BR_M02_05_longTerm_isRemindedFromThirtyDaysBefore_untilTheEndDate() {
        LocalDate day = today();
        long within = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 30);
        long onTheLastDay = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 0);
        long tooEarly = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 31);

        runOn(day);

        assertThat(remindersOf(within, EXPIRING)).hasSize(1);
        assertThat(remindersOf(within, EXPIRING).get(0).on()).isEqualTo(day);
        assertThat(remindersOf(onTheLastDay, EXPIRING)).hasSize(1);
        assertThat(remindersOf(tooEarly, EXPIRING)).isEmpty();
    }
}
