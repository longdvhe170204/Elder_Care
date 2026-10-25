package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.ContractOverdueEscalated;
import com.cambae.backend.m02.api.events.M02Reminder;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 004 T031, T036 (US3; FR-029, BR-M02-05, CFG-M02-10, Q-20): hợp đồng qua ngày kết thúc mà người cao tuổi vẫn lưu trú thì
 * giữ Hiệu lực và mang dấu "quá hạn hợp đồng" từ ngày liền sau; Hành chính được nhắc mỗi ngày; quá CFG-M02-10 ngày thì
 * báo Quản lý viện, một lần cho mỗi hợp đồng; phụ lục gia hạn gỡ dấu.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_029_ContractOverdueIT extends ContractTestBase {

    private static final String OVERDUE = "CONTRACT_OVERDUE";

    @Autowired
    ApplicationEvents applicationEvents;

    /** Hợp đồng Hiệu lực có ngày kết thúc hiện hành {@code end}, người cao tuổi ở {@code residentStatus}. */
    private long contractEnding(ResidenceType type, LocalDate end, String residentStatus) {
        long residentId = admissionResident();
        setStatus(residentId, residentStatus);
        long id = contracts.contract(residentId, ContractStatus.ACTIVE, type, CareLevel.BASIC, end.minusDays(200));
        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", end, id);
        return id;
    }

    private void runOn(LocalDate day) {
        noonOf(day);
        dailyRunner.catchUp();
    }

    private LocalDate overdueSince(long id) {
        return jdbc.queryForObject("SELECT overdue_since FROM contract WHERE id = ?", LocalDate.class, id);
    }

    private List<M02Reminder> overdueReminders(long id) {
        return applicationEvents.stream(M02Reminder.class)
                .filter(reminder -> OVERDUE.equals(reminder.type()) && reminder.subjectId() == id)
                .toList();
    }

    private List<ContractOverdueEscalated> escalations(long id) {
        return applicationEvents.stream(ContractOverdueEscalated.class)
                .filter(event -> event.contractId() == id)
                .toList();
    }

    @Test
    void FR_029_theDayAfterTheEndDate_theContractStaysActive_withTheOverdueMark() throws Exception {
        LocalDate end = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE");

        runOn(end);
        assertThat(overdueSince(id)).as("chưa qua ngày kết thúc").isNull();

        runOn(end.plusDays(1));

        assertThat(contractStatus(id)).isEqualTo("ACTIVE");
        assertThat(overdueSince(id)).isEqualTo(end.plusDays(1));
        assertThat(jdbc.queryForObject("SELECT current_end_date FROM contract WHERE id = ?", LocalDate.class, id))
                .as("ngày kết thúc không đổi, không tự gia hạn")
                .isEqualTo(end);
        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueSince").value(end.plusDays(1).toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void FR_029_theMarkIsDatedByTheDayItShouldHaveBeenSet_notByWhenTheJobRan() {
        LocalDate end = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE");

        runOn(end.plusDays(4));

        assertThat(overdueSince(id)).isEqualTo(end.plusDays(1));
    }

    @Test
    void FR_029_overdueContracts_listFilterShowsThem() throws Exception {
        LocalDate end = today();
        long overdue = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE");
        long fine = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end.plusDays(50), "IN_RESIDENCE");
        runOn(end.plusDays(2));

        user.set(office.user());
        String listed = mvc.perform(get("/api/v1/contracts").param("overdue", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(listed).contains("\"id\":" + overdue).doesNotContain("\"id\":" + fine);
    }

    @Test
    void FR_029_theOfficeIsRemindedEveryDayWhileOverdue() {
        LocalDate end = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE");

        runOn(end.plusDays(1));
        runOn(end.plusDays(2));
        runOn(end.plusDays(3));

        assertThat(overdueReminders(id))
                .extracting(M02Reminder::on)
                .containsExactly(end.plusDays(1), end.plusDays(2), end.plusDays(3));
    }

    @Test
    void FR_029_afterMoreThanTheThresholdDays_theManagerIsToldOnce() {
        LocalDate end = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE");

        for (int day = 1; day <= 7; day++) {
            runOn(end.plusDays(day));
        }
        assertThat(escalations(id)).as("7 ngày chưa vượt ngưỡng CFG-M02-10").isEmpty();

        runOn(end.plusDays(8));
        runOn(end.plusDays(9));
        runOn(end.plusDays(10));

        assertThat(escalations(id)).singleElement().satisfies(event -> {
            assertThat(event.days()).isEqualTo(8);
            assertThat(event.residentId())
                    .isEqualTo(jdbc.queryForObject("SELECT resident_id FROM contract WHERE id = ?", Long.class, id));
        });
        assertThat(jdbc.queryForObject(
                        "SELECT manager_notified_overdue_at FROM contract WHERE id = ?", java.sql.Timestamp.class, id))
                .isNotNull();
    }

    @Test
    void FR_029_anExtendingAddendum_removesTheMark() {
        LocalDate end = today();
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE");
        runOn(end.plusDays(3));
        assertThat(overdueSince(id)).isNotNull();

        // Phụ lục gia hạn (US5) đặt lại ngày kết thúc hiện hành; việc hằng ngày gỡ dấu ở lần chạy kế tiếp
        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", end.plusDays(300), id);
        runOn(end.plusDays(4));

        assertThat(overdueSince(id)).isNull();
        assertThat(overdueReminders(id)).hasSize(1);
    }

    @Test
    void FR_029_onlyResidentsStillStaying_areMarked() {
        LocalDate end = today();
        List<Long> staying = List.of(
                contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "IN_RESIDENCE"),
                contractEnding(ResidenceType.SHORT_TERM_RESIDENTIAL, end, "TEMPORARILY_ABSENT"),
                contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "HOSPITALIZED"));
        List<Long> notStaying = List.of(
                contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "RESIDENCY_ENDED"),
                contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, end, "DECEASED"));

        runOn(end.plusDays(2));

        staying.forEach(id -> assertThat(overdueSince(id)).isEqualTo(end.plusDays(1)));
        notStaying.forEach(id -> assertThat(overdueSince(id)).isNull());
    }

    @Test
    void FR_029_aContractWithoutAnEndDate_isNeverOverdue() {
        long id = contractEnding(ResidenceType.LONG_TERM_RESIDENTIAL, today(), "IN_RESIDENCE");
        jdbc.update("UPDATE contract SET current_end_date = NULL WHERE id = ?", id);

        runOn(today().plusDays(30));

        assertThat(overdueSince(id)).isNull();
    }
}
