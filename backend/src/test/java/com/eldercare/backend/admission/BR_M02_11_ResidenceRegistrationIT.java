package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 018 T006 ({@code BR_M02_11_KhaiBaoIT}; US1, quickstart Q1 → Q6; FR-001, FR-001a, FR-002, FR-009, FR-010; BR-M02-11,
 * Q-215, Q-306): Hoàn tất tiếp nhận nội trú ghi nghĩa vụ và nhắc Hành chính, không tự tạo khai báo; Hành chính lập khai
 * báo, loại theo thời gian lưu trú dự kiến và CFG-M02-11, hạn theo mốc; quá hạn nhắc Hành chính mỗi ngày, báo Quản lý viện
 * một lần; bán trú không có nghĩa vụ; xác nhận thường trú cùng xã/phường đổi loại, giữ hạn.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_11_ResidenceRegistrationIT extends RegistrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private ResidenceRegistration create(long residentId) {
        user.set(office.user());
        return registrations.createRegistration(ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    private List<ResidenceRegistrationReminder> reminders(long residentId) {
        return events.stream(ResidenceRegistrationReminder.class)
                .filter(event -> event.residentId() == residentId)
                .toList();
    }

    // ---- Q1, Q2: nghĩa vụ, nhắc lập, loại khai báo ---------------------------------------------------------------

    @Test
    void Q1_longTermAdmission_recordsAnObligationAndReminds_withoutCreatingARegistration() throws Exception {
        long residentId = readyWith(
                ResidenceType.LONG_TERM_RESIDENTIAL, today().plusYears(1).minusDays(1));
        completeAdmission(residentId);

        Map<String, Object> obligation = awaitObligations(residentId, 1).getFirst();
        assertThat(obligation.get("trigger_type")).isEqualTo("ADMISSION_COMPLETED");
        assertThat(dateOf(obligation.get("due_at")))
                .as("mốc + CFG-M02-12 (1 ngày)")
                .isEqualTo(today().plusDays(1));
        assertThat(obligation.get("registration_id")).isNull();
        assertThat(registrationCount(residentId))
                .as("Q-306: hệ thống không tự tạo khai báo")
                .isZero();
        assertThat(reminderCount("CREATE", residentId))
                .as("nhắc lập một lần (FR-010)")
                .isEqualTo(1);

        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId == %d)].taskType".formatted(residentId))
                        .value("TO_CREATE"));
    }

    // ---- Q3: bán trú --------------------------------------------------------------------------------------------

    // ---- Q4: quá hạn -------------------------------------------------------------------------------------------

    // ---- Q5: không chặn ---------------------------------------------------------------------------------------

    // ---- Q6: thường trú cùng xã/phường ------------------------------------------------------------------------

    // ---- FR-001a: bán trú chuyển nội trú bằng hợp đồng mới ------------------------------------------------------

    private void contractActivated(long contractId, long residentId) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(new ContractStatusChanged(
                contractId, residentId, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE, null)));
    }

    // ---- FR-009: quyền ------------------------------------------------------------------------------------------
}
