package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m02.api.events.AdmissionCompleted;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Audit Feature 004 (P1, quyết định chủ dự án 2026-10-27; FR-001, FR-004, FR-074, FR-075): Hoàn tất tiếp nhận hợp lệ không
 * cần lượt đăng ký mở. Luôn ghi ngày vào ở ({@code resident_residency.admitted_on}) và phát {@link AdmissionCompleted}
 * trong transaction của lệnh, với ngày kết thúc hiện hành của hợp đồng (kể cả sau phụ lục gia hạn). Lệnh thất bại hoặc lặp
 * lại không để lại ngày hay sự kiện.
 */
@AutoConfigureMockMvc
@WithMockUser
class AdmissionCompletedEventIT extends ContractTestBase {

    private void application(long residentId) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
    }

    /** Hợp đồng Hiệu lực (nội trú dài hạn) bắt đầu hôm nay, có {@code endDate} nếu truyền; trả mã hợp đồng. */
    private long signedContract(long residentId, LocalDate endDate) throws Exception {
        user.set(office.user());
        ObjectNode body = body(residentId);
        if (endDate != null) {
            body.put("endDate", endDate.toString());
        }
        long id = create(body).get("contract").get("id").asLong();
        send(id);
        recordSignedRaw(id, signedBody(residentId, today())).andExpect(status().isOk());
        return id;
    }

    private void complete(long residentId) {
        user.set(office.user());
        statusService.completeAdmission(
                ResidentStatusService.target(residentId),
                new Reason("Hoàn tất thử"),
                JsonNodeFactory.instance.objectNode());
    }

    private List<AdmissionCompleted> completedEvents(long residentId) {
        return events.stream(AdmissionCompleted.class)
                .filter(event -> event.residentId() == residentId)
                .toList();
    }

    private LocalDate admittedOn(long residentId) {
        return jdbc
                .queryForList(
                        "SELECT admitted_on FROM resident_residency WHERE resident_id = ?", Date.class, residentId)
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(Date::toLocalDate)
                .findFirst()
                .orElse(null);
    }

    @Test
    void withAnOpenApplication_admissionCompletes_recordsTheDate_andPublishesTheEvent() throws Exception {
        long residentId = residentWithRepresentative();
        application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);
        long contractId = signedContract(residentId, null);

        complete(residentId);

        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(admittedOn(residentId)).isEqualTo(today());
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM admission_application WHERE resident_id = ?", String.class, residentId))
                .isEqualTo("ADMITTED");
        assertThat(completedEvents(residentId)).singleElement().satisfies(event -> {
            assertThat(event.contractId()).isEqualTo(contractId);
            assertThat(event.startDate()).isEqualTo(today());
        });
    }

    @Test
    void withoutAnOpenApplication_admissionStillCompletes_recordsTheDate_andPublishesTheEvent() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.BASIC);
        long contractId = signedContract(residentId, null);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_application WHERE resident_id = ?", Integer.class, residentId))
                .isZero();

        complete(residentId);

        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(admittedOn(residentId)).isEqualTo(today());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_application WHERE resident_id = ?", Integer.class, residentId))
                .as("không tự tạo lượt đăng ký giả")
                .isZero();
        assertThat(completedEvents(residentId)).singleElement().satisfies(event -> {
            assertThat(event.contractId()).isEqualTo(contractId);
            assertThat(event.startDate()).isEqualTo(today());
        });
    }

    @Test
    void aFailedCommand_leavesNeitherTheDateNorTheEvent() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.BASIC);

        assertThatThrownBy(() -> complete(residentId)).isInstanceOf(RuntimeException.class);

        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
        assertThat(admittedOn(residentId)).isNull();
        assertThat(completedEvents(residentId)).isEmpty();
    }

    @Test
    void retryingTheCommand_isRefused_withoutADuplicateEventOrChangedDate() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.BASIC);
        signedContract(residentId, null);
        complete(residentId);
        LocalDate first = admittedOn(residentId);

        assertThatThrownBy(() -> complete(residentId)).isInstanceOf(RuntimeException.class);

        assertThat(completedEvents(residentId)).hasSize(1);
        assertThat(admittedOn(residentId)).isEqualTo(first);
    }

    @Test
    void anExtendedContract_publishesTheCurrentEndDate() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.BASIC);
        LocalDate original = today().plusDays(60);
        LocalDate extended = today().plusDays(120);
        long contractId = signedContract(residentId, original);
        WaitingListTestBase.admin("UPDATE contract SET current_end_date = '" + extended + "' WHERE id = " + contractId);

        complete(residentId);

        assertThat(completedEvents(residentId))
                .singleElement()
                .satisfies(event -> assertThat(event.expectedEndDate()).isEqualTo(extended));
    }

    @Test
    void aContractNotExtended_publishesItsEndDate() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.BASIC);
        LocalDate end = today().plusDays(60);
        signedContract(residentId, end);

        complete(residentId);

        assertThat(completedEvents(residentId))
                .singleElement()
                .satisfies(event -> assertThat(event.expectedEndDate()).isEqualTo(end));
    }
}
