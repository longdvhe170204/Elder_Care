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
class BR_10_ContractReminderIT extends ContractTestBase {

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
    void BR_10_longTerm_isRemindedFromThirtyDaysBefore_untilTheEndDate() {
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

    private static String text(com.eldercare.backend.common.notification.NotificationRequest request) {
        return request.content().getFirst().content();
    }

    /**
     * Demo 2026-11-25 (góp ý nhóm, E làm, chờ B, A duyệt): nhắc Hành chính có ngày hết hạn và việc cần làm; người đại diện
     * được báo một lần khi vào CFG-M02-04 ngày trước hạn (không lặp mỗi ngày).
     */
    @Test
    void BR_10_firstDayOfWindow_officeTextHasEndDate_andRepresentativeIsToldOnce() {
        LocalDate day = today();
        long first = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 30);
        long later = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 20);
        String end = day.plusDays(30).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        runOn(day);

        var requests = remindersOf(first, EXPIRING).getFirst().notificationRequests();
        assertThat(text(requests.getFirst()))
                .contains("hết hạn ngày " + end)
                .contains("Lập hồ sơ kết thúc lưu trú hoặc điều chỉnh hợp đồng");
        assertThat(requests)
                .anySatisfy(r -> assertThat(r.recipientGroups())
                        .anyMatch(g ->
                                g instanceof com.eldercare.backend.common.notification.RecipientGroup.Representative));
        assertThat(remindersOf(later, EXPIRING).getFirst().notificationRequests())
                .anySatisfy(r -> assertThat(r.recipientGroups())
                        .anyMatch(g ->
                                g instanceof com.eldercare.backend.common.notification.RecipientGroup.Representative));

        runOn(day.plusDays(1));

        assertThat(remindersOf(first, EXPIRING)).hasSize(2);
        assertThat(remindersOf(first, EXPIRING).getLast().notificationRequests())
                .hasSize(1);
        assertThat(remindersOf(later, EXPIRING).getLast().notificationRequests())
                .hasSize(1);
    }

    /**
     * Demo 2026-11-25: job nhắc chỉ xét ngày mới nhất khi chạy bù (M02ReminderTask.latestDayOnly), nên báo người đại diện
     * gắn vào lần nhắc quá hạn đầu tiên chứ không vào đúng ngày đầu quá hạn: hợp đồng đã quá hạn 3 ngày, nhắc lần đầu vẫn
     * báo người đại diện, ngày sau không báo lại.
     */
    @Test
    void BR_10_overdueFoundLate_representativeIsToldOnTheFirstOverdueReminder() {
        LocalDate day = today();
        long overdue = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, -3);
        long residentId = jdbc.queryForObject("SELECT resident_id FROM contract WHERE id = ?", Long.class, overdue);
        setStatus(residentId, "IN_RESIDENCE");

        runOn(day);

        assertThat(remindersOf(overdue, "CONTRACT_OVERDUE")).hasSize(1);
        assertThat(remindersOf(overdue, "CONTRACT_OVERDUE").getFirst().notificationRequests())
                .anySatisfy(r -> assertThat(r.recipientGroups())
                        .anyMatch(g ->
                                g instanceof com.eldercare.backend.common.notification.RecipientGroup.Representative));

        runOn(day.plusDays(1));

        assertThat(remindersOf(overdue, "CONTRACT_OVERDUE")).hasSize(2);
        assertThat(remindersOf(overdue, "CONTRACT_OVERDUE").getLast().notificationRequests())
                .hasSize(1);
    }
}
