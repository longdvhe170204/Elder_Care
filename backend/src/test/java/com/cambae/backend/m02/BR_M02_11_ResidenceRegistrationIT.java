package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.AdmissionCompleted;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder.Kind;
import com.cambae.backend.m02.internal.registration.domain.RegistrationStatus;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Test
    void Q1_creatingLate_keepsTheObligationDueDate() {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        Map<String, Object> obligation = awaitObligations(residentId, 1).getFirst();
        clock.setTo(time.now().plus(Duration.ofDays(2)));

        ResidenceRegistration created = create(residentId);

        assertThat(created.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        assertThat(created.status()).isEqualTo(RegistrationStatus.TO_SUBMIT);
        assertThat(created.renewal()).isFalse();
        assertThat(dateOf(obligations(residentId).getFirst().get("due_at")))
                .isEqualTo(dateOf(obligation.get("due_at")));
        assertThat(created.dueAt()).as("lập muộn không kéo dài hạn (FR-002)").isBefore(time.now());
        assertThat(obligations(residentId).getFirst().get("registration_id")).isEqualTo(created.id());
    }

    @Test
    void Q2_shortTermFourteenDays_isAStayNotification() throws Exception {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);

        mvc.perform(post("/api/v1/residents/" + residentId + "/residence-registrations")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("STAY_NOTIFICATION"))
                .andExpect(jsonPath("$.status").value("TO_SUBMIT"))
                .andExpect(jsonPath("$.residentId").value(residentId))
                .andExpect(jsonPath("$.renewal").value(false))
                .andExpect(jsonPath("$.localPermanentResident").value(false));
    }

    @Test
    void BR_M02_11_thirtyDaysCountingBothEnds_reachesTheThreshold() {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(29));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);

        assertThat(create(residentId).type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    // ---- Q3: bán trú --------------------------------------------------------------------------------------------

    @Test
    void Q3_dayCare_hasNoObligationNoReminder_andCreatingIsBlocked() throws Exception {
        long residentId = readyWith(ResidenceType.DAY_CARE, null);
        completeAdmission(residentId);
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        // Chờ một người nội trú khác qua listener để chắc listener của người bán trú cũng đã chạy xong
        long other = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(other);
        awaitObligations(other, 1);

        assertThat(obligations(residentId)).isEmpty();
        assertThat(reminderCount("CREATE", residentId)).isZero();
        mvc.perform(post("/api/v1/residents/" + residentId + "/residence-registrations")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_RESIDENTIAL_CARE"));
        assertThat(registrationCount(residentId)).isZero();
    }

    // ---- Q4: quá hạn -------------------------------------------------------------------------------------------

    @Test
    void Q4_overdue_remindsTheOfficeEveryDay_andTheManagerOnce() {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        LocalDate due = dateOf(awaitObligations(residentId, 1).getFirst().get("due_at"));

        overdueReminders.remind(due);
        assertThat(reminders(residentId)).as("ngày hạn chưa quá hạn").isEmpty();

        create(residentId);
        overdueReminders.remind(due.plusDays(1));
        overdueReminders.remind(due.plusDays(1));
        overdueReminders.remind(due.plusDays(2));

        assertThat(reminders(residentId))
                .as("đã lập nhưng chưa ghi nộp vẫn nhắc; chạy lại cùng ngày không nhắc trùng")
                .extracting(ResidenceRegistrationReminder::kind, ResidenceRegistrationReminder::on)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(Kind.OVERDUE, due.plusDays(1)),
                        org.assertj.core.groups.Tuple.tuple(Kind.OVERDUE_MANAGER, due.plusDays(1)),
                        org.assertj.core.groups.Tuple.tuple(Kind.OVERDUE, due.plusDays(2)));
        assertThat(obligations(residentId).getFirst().get("manager_notified_at"))
                .isNotNull();
    }

    // ---- Q5: không chặn ---------------------------------------------------------------------------------------

    @Test
    void Q5_admissionWithoutAnyRegistration_isNotBlocked() {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        assertThat(failure(() -> completeAdmission(residentId))).isNull();
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    // ---- Q6: thường trú cùng xã/phường ------------------------------------------------------------------------

    @Test
    void Q6_confirmingLocalPermanentResident_switchesTheType_andKeepsTheDueDate() throws Exception {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        ResidenceRegistration created = create(residentId);

        mvc.perform(post("/api/v1/residence-registrations/" + created.id() + "/confirm-local-permanent-resident")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basisNote\": \"CCCD: thường trú phường Bến Nghé\"}"))
                .andExpect(status().isOk());

        ResidenceRegistration after = registrations
                .ofResident(ResidenceRegistrationService.targetForResident(residentId), residentId)
                .getFirst();
        assertThat(after.type()).isEqualTo(RegistrationType.STAY_NOTIFICATION);
        assertThat(after.localPermanentResident()).isTrue();
        assertThat(after.dueAt()).isEqualTo(created.dueAt());
        assertThat(jdbc.queryForObject(
                        "SELECT local_permanent_resident FROM registration_resident WHERE resident_id = ?",
                        Boolean.class,
                        residentId))
                .as("khai báo sau của hồ sơ dùng Thông báo lưu trú")
                .isTrue();
    }

    @Test
    void Q6_confirmingWithoutABasisNote_isRejected() {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        ResidenceRegistration created = create(residentId);

        assertThat(failure(() -> registrations.confirmLocalPermanentResident(
                        registrations.targetOf(created.id()), created.id(), " ")))
                .isInstanceOf(InvalidValueException.class);
    }

    // ---- FR-001a: bán trú chuyển nội trú bằng hợp đồng mới ------------------------------------------------------

    private void contractActivated(long contractId, long residentId) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(new ContractStatusChanged(
                contractId, residentId, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE, null)));
    }

    @Test
    void FR_001a_newResidentialContractOfADayCareResident_recordsAnObligationFromItsStartDate() {
        long residentId = readyWith(ResidenceType.DAY_CARE, today().plusDays(9));
        completeAdmission(residentId);
        LocalDate start = today().plusDays(10);
        long contractId = contract(
                residentId, ContractStatus.ACTIVE, ResidenceType.SHORT_TERM_RESIDENTIAL, start, start.plusDays(59));

        contractActivated(contractId, residentId);

        Map<String, Object> obligation = awaitObligations(residentId, 1).getFirst();
        assertThat(obligation.get("trigger_type")).isEqualTo("RESIDENTIAL_EFFECTIVE");
        assertThat(((Number) obligation.get("source_id")).longValue()).isEqualTo(contractId);
        assertThat(dateOf(obligation.get("due_at")))
                .as("hạn tính từ ngày phần nội trú có hiệu lực, kể cả ở tương lai (Q-336)")
                .isEqualTo(start.plusDays(1));
        assertThat(registrationCount(residentId)).isZero();
        ResidenceRegistration created = create(residentId);
        assertThat(created.type())
                .as("60 ngày từ ngày hiệu lực phần nội trú")
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void FR_001a_contractOfAResidentWhoIsNotDayCare_recordsNothing() {
        long admitted = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(9));
        completeAdmission(admitted);
        awaitObligations(admitted, 1);
        long next = contract(
                admitted,
                ContractStatus.ACTIVE,
                ResidenceType.SHORT_TERM_RESIDENTIAL,
                today().plusDays(10),
                today().plusDays(20));
        long pending = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        long pendingContract = jdbc.queryForObject(
                "SELECT MAX(id) FROM contract WHERE resident_id = ? AND status = 'ACTIVE'", Long.class, pending);

        contractActivated(next, admitted);
        contractActivated(pendingContract, pending);
        // Mốc chắc chắn sau hai sự kiện trên: một người nội trú khác vào ở
        long other = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(other);
        awaitObligations(other, 1);

        assertThat(obligations(admitted)).as("đã nội trú: không phải FR-001a").hasSize(1);
        assertThat(obligations(pending))
                .as("chưa Hoàn tất tiếp nhận: chờ AdmissionCompleted")
                .isEmpty();
    }

    @Test
    void NFR_05_aRepeatedEvent_recordsOneObligationAndOneReminder() {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        Map<String, Object> obligation = awaitObligations(residentId, 1).getFirst();
        long contractId = ((Number) obligation.get("source_id")).longValue();

        transaction.executeWithoutResult(status -> publisher.publishEvent(
                new AdmissionCompleted(residentId, contractId, ResidenceType.LONG_TERM_RESIDENTIAL, today(), null)));
        long other = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(other);
        awaitObligations(other, 1);

        assertThat(obligations(residentId)).hasSize(1);
        assertThat(reminderCount("CREATE", residentId)).isEqualTo(1);
    }

    // ---- FR-009: quyền ------------------------------------------------------------------------------------------

    @Test
    void FR_009_theManagerViews_butCannotCreate_otherRolesCannotView() throws Exception {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);

        user.set(manager.user());
        mvc.perform(get("/api/v1/residence-registrations/to-do")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/residents/" + residentId + "/residence-registrations"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/residents/" + residentId + "/residence-registrations")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        user.set(users.staff(Role.NURSING_STAFF).user());
        mvc.perform(get("/api/v1/residence-registrations/to-do")).andExpect(status().isForbidden());
        assertThat(registrationCount(residentId)).isZero();
    }

    @Test
    void FR_001_creatingWithoutAnObligation_isBlocked() {
        long residentId = admissionResident();
        contract(residentId, ContractStatus.ACTIVE, ResidenceType.LONG_TERM_RESIDENTIAL, today(), null);
        setStatus(residentId, "IN_RESIDENCE");

        BusinessException e = failure(() -> create(residentId));
        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.errorCode()).isEqualTo("NO_REGISTRATION_OBLIGATION");
    }
}
