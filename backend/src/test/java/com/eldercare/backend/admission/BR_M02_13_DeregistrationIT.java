package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder.Kind;
import com.eldercare.backend.admission.internal.registration.domain.ObligationTrigger;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionChannel;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.service.RegistrationDeregistration;
import com.eldercare.backend.admission.internal.registration.service.RegistrationOverdueReminders;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService.Task;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * 018 T013 ({@code BR_M02_13_KhaiBaoXoaIT}; US4 #1 → #4, quickstart Q18, Q19; FR-005, FR-008, BR-M02-13, SC-004; research
 * Z-05): lệnh Kết thúc lưu trú, Ghi nhận qua đời thật của 001 đi qua bean tham gia của 018 (không điều kiện chặn); khai báo
 * đang xử lý thành Đã hủy, nghĩa vụ đang mở hủy; người có khai báo Đã xác nhận hoặc Hết hạn được nhắc khai báo xóa ngay và
 * mỗi CFG-M02-09; Hành chính ghi đã khai báo xóa kèm bằng chứng khi hồ sơ ở trạng thái cuối. Nhắc hằng ngày gọi thẳng
 * service vì job chưa gắn (BL-90).
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_13_DeregistrationIT extends DeathTestBase {

    @Autowired
    ResidenceRegistrationService registrations;

    @Autowired
    RegistrationDeregistration deregistration;

    @Autowired
    RegistrationOverdueReminders overdueReminders;

    @Autowired
    RegistrationObligationStore obligationStore;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    TransactionTemplate transaction;

    // ---- Dữ liệu ----------------------------------------------------------------------------------------------------

    private long evidence() {
        return files.upload(
                "bang-chung.pdf",
                FileTypes.PDF,
                "%PDF-1.4 bang chung".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    /** Nghĩa vụ khai báo (bảng kỹ thuật) như listener Hoàn tất tiếp nhận ghi; hạn {@code dueAt}. */
    private void obligation(long residentId, Instant dueAt) {
        long contractId =
                jdbc.queryForObject("SELECT MAX(id) FROM contract WHERE resident_id = ?", Long.class, residentId);
        transaction.executeWithoutResult(s -> obligationStore.record(
                residentId, ObligationTrigger.ADMISSION_COMPLETED, time.now(), dueAt, contractId));
    }

    /** Khai báo Cần khai báo do Hành chính lập (Q-306). */
    private ResidenceRegistration toSubmit(long residentId) {
        obligation(residentId, time.now().plus(Duration.ofDays(30)));
        user.set(office.user());
        return registrations.createRegistration(ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    private void submit(ResidenceRegistration registration) {
        user.set(office.user());
        registrations.recordSubmitted(
                registrations.targetOf(registration.id()),
                registration.id(),
                new ResidenceRegistrationService.Submission(
                        today(), SubmissionChannel.IN_PERSON, "Công an phường X", null));
    }

    /** Khai báo Đã xác nhận (Đăng ký tạm trú của người nội trú dài hạn). */
    private ResidenceRegistration confirmed(long residentId) {
        ResidenceRegistration registration = toSubmit(residentId);
        assertThat(registration.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        submit(registration);
        registrations.recordAccepted(
                registrations.targetOf(registration.id()),
                registration.id(),
                new ResidenceRegistrationService.Acceptance(evidence(), today().plusYears(1)));
        return registration;
    }

    private void deregister(long id, Long evidenceFileId) {
        user.set(office.user());
        registrations.recordDeregistered(registrations.targetOf(id), id, evidenceFileId);
    }

    private String evidenceBody(Long evidenceFileId) {
        return evidenceFileId == null ? "{}" : "{\"evidenceFileId\":" + evidenceFileId + "}";
    }

    private String url(long id) {
        return "/api/v1/residence-registrations/" + id + "/record-deregistered";
    }

    private String statusOf(long id) {
        return RegistrationSql.status(jdbc, id);
    }

    private int registrationCount(long residentId) {
        return RegistrationSql.registrationCount(jdbc, residentId);
    }

    private int reminderCount(long residentId) {
        return RegistrationSql.reminderCount(jdbc, "DEREGISTER", residentId);
    }

    private List<ResidenceRegistrationReminder> deregisterReminders(long residentId) {
        return events.stream(ResidenceRegistrationReminder.class)
                .filter(event -> event.residentId() == residentId && event.kind() == Kind.DEREGISTER)
                .toList();
    }

    private int auditCount(String action, long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND entity_type = 'M02.RESIDENCE_REGISTRATION'"
                        + " AND entity_id = ?",
                Integer.class,
                action,
                String.valueOf(id));
    }

    private List<Task> tasksOf(long residentId) {
        user.set(office.user());
        return registrations.toDo(Target.UNSPECIFIED).stream()
                .filter(task -> task.residentId() == residentId)
                .toList();
    }

    private StatusTransitionParticipant registrationParticipant() {
        return participants.stream()
                .filter(p -> p.getClass().getSimpleName().equals("RegistrationStatusParticipant"))
                .findFirst()
                .orElseThrow();
    }

    /** Kết thúc lưu trú thật (hồ sơ kết thúc, bàn giao, đồng hồ trưa {@code d0}); trả người cao tuổi. */
    private long endedWith(java.util.function.LongFunction<ResidenceRegistration> registration) throws Exception {
        ReadyEnding ready = readyToEnd(d0);
        registration.apply(ready.residentId());
        endResidency(ready.residentId(), ready.endingId(), time.now());
        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
        return ready.residentId();
    }

    // ---- SC-004: không chặn ---------------------------------------------------------------------------------------

    // ---- Q18: Kết thúc lưu trú khi có khai báo Đã xác nhận -------------------------------------------------------

    @Test
    void US4_2_recordDeregistered_withEvidence_whileTheProfileIsFinal_movesToDeregistered_andStopsReminders()
            throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        long residentId = endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        long id = holder.getFirst().id();
        int rows = registrationCount(residentId);
        long file = evidence();

        postAs(office, url(id), evidenceBody(file)).andExpect(status().isOk());

        assertThat(statusOf(id)).isEqualTo("DEREGISTERED");
        JsonNode content = RegistrationSql.content(jdbc, id);
        assertThat(content.hasNonNull("deregisteredAt")).isTrue();
        assertThat(content.get("deregisteredById").asLong()).isEqualTo(office.accountId());
        assertThat(content.get("deregistrationEvidenceFileId").asLong()).isEqualTo(file);
        assertThat(content.hasNonNull("evidenceFileId"))
                .as("bằng chứng chấp nhận giữ nguyên")
                .isTrue();
        assertThat(auditCount("M02_REGISTRATION_DEREGISTERED", id)).isEqualTo(1);
        assertThat(registrationCount(residentId))
                .as("không xóa, không thêm dòng")
                .isEqualTo(rows);
        assertThat(tasksOf(residentId)).as("hết việc cần khai báo xóa").isEmpty();

        clockAt(d0.plusDays(1), 9, 0);
        deregistration.remind(d0.plusDays(1));
        assertThat(reminderCount(residentId)).as("nhắc dừng").isEqualTo(1);

        postAs(office, url(id), evidenceBody(evidence()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    // ---- Q19: qua đời khi khai báo đang xử lý ----------------------------------------------------------------------

    // ---- BL-97 A: không lập khai báo gia hạn khi hồ sơ ở trạng thái cuối (BR-M01-05) ------------------------------

    private int auditCountOfResident(String action, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND resident_id = ?",
                Integer.class,
                action,
                residentId);
    }

    // ---- BL-97 B (A duyet 2026-11-05): mot thu tuc khai bao xoa theo nguoi (FR-008, bang 6.10) ---------------------

    // ---- US4 #4: nhắc mỗi CFG-M02-09 --------------------------------------------------------------------------------

    // ---- Phát lại, đồng thời ----------------------------------------------------------------------------------------
}
