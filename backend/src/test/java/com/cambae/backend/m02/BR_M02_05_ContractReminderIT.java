package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.M02Reminder;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
        dailyRunner.catchUp();
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

    @Test
    void BR_M02_05_dayCareContract_isRemindedLikeALongTermOne() {
        LocalDate day = today();
        long id = contractEnding(ResidenceType.DAY_CARE, day, 10);

        runOn(day);

        assertThat(remindersOf(id, EXPIRING)).hasSize(1);
        assertThat(remindersOf(id, SHORT_TERM)).isEmpty();
    }

    @Test
    void BR_M02_05_oncePerDay_notTwice_andAgainTheNextDay() {
        LocalDate day = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 20);

        runOn(day);
        dailyRunner.catchUp();
        assertThat(remindersOf(id, EXPIRING)).hasSize(1);

        runOn(day.plusDays(1));
        assertThat(remindersOf(id, EXPIRING)).hasSize(2);
        assertThat(remindersOf(id, EXPIRING).get(1).on()).isEqualTo(day.plusDays(1));
    }

    @Test
    void BR_M02_05_afterAnExtension_theReminderStops() {
        LocalDate day = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 20);
        runOn(day);
        assertThat(remindersOf(id, EXPIRING)).hasSize(1);

        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", day.plusDays(400), id);
        runOn(day.plusDays(1));

        assertThat(remindersOf(id, EXPIRING)).hasSize(1);
    }

    @Test
    void BR_M02_05_shortTerm_isRemindedOnlyOnTheEndDate() {
        LocalDate day = today();
        long endsToday = contractEnding(ResidenceType.SHORT_TERM_RESIDENTIAL, day, 0);
        long endsTomorrow = contractEnding(ResidenceType.SHORT_TERM_RESIDENTIAL, day, 1);

        runOn(day);

        assertThat(remindersOf(endsToday, SHORT_TERM)).hasSize(1);
        assertThat(remindersOf(endsToday, EXPIRING)).isEmpty();
        assertThat(remindersOf(endsTomorrow, SHORT_TERM)).isEmpty();
        assertThat(remindersOf(endsTomorrow, EXPIRING)).isEmpty();
    }

    @Test
    void BR_M02_05_theSystemNeverExtendsOrEndsAContract() {
        LocalDate day = today();
        long id = contractEnding(ResidenceType.SHORT_TERM_RESIDENTIAL, day, 0);

        runOn(day);
        runOn(day.plusDays(1));

        assertThat(contractStatus(id)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT current_end_date FROM contract WHERE id = ?", LocalDate.class, id))
                .isEqualTo(day);
    }

    @Test
    void BR_M02_05_onlyActiveContractsAreReminded() {
        LocalDate day = today();
        List<ContractStatus> notActive = List.of(
                ContractStatus.DRAFT,
                ContractStatus.PENDING_SIGNATURE,
                ContractStatus.ENDED,
                ContractStatus.TERMINATED,
                ContractStatus.CANCELLED);
        List<Long> ids = notActive.stream()
                .map(status -> {
                    long id = contracts.contract(
                            admissionResident(),
                            status,
                            ResidenceType.LONG_TERM_RESIDENTIAL,
                            CareLevel.BASIC,
                            day.minusDays(100));
                    jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", day.plusDays(5), id);
                    return id;
                })
                .toList();

        runOn(day);

        ids.forEach(id -> assertThat(remindersOf(id, EXPIRING)).isEmpty());
    }

    @Test
    void CFG_M02_04_theWindowFollowsTheParameter() throws Exception {
        LocalDate day = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, day, 20);
        String etag = null;
        try {
            user.set(manager.user());
            etag = mvc.perform(get("/api/v1/parameters/CFG-M02-04"))
                    .andReturn()
                    .getResponse()
                    .getHeader("ETag");
            mvc.perform(post("/api/v1/parameters/CFG-M02-04/change-value")
                            .with(csrf())
                            .header("If-Match", etag)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\":{\"value\":10,\"unit\":\"DAYS\"},\"reason\":\"Thu hẹp cho test\"}"))
                    .andExpect(status().isOk());
            user.set(office.user());

            runOn(day);
            assertThat(remindersOf(id, EXPIRING)).isEmpty();

            runOn(day.plusDays(10));
            assertThat(remindersOf(id, EXPIRING)).hasSize(1);
        } finally {
            clock.reset();
            user.set(manager.user());
            String current = mvc.perform(get("/api/v1/parameters/CFG-M02-04"))
                    .andReturn()
                    .getResponse()
                    .getHeader("ETag");
            mvc.perform(post("/api/v1/parameters/CFG-M02-04/change-value")
                    .with(csrf())
                    .header("If-Match", current)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"restoreDefault\":true,\"reason\":\"khôi phục sau test\"}"));
            user.set(office.user());
        }
    }
}
