package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m02.api.events.AdmissionCompleted;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T014, T033 (FR-004 (b), (c), (e), FR-075, BR-M02-22): điều kiện tiếp nhận do 004 kiểm đọc hợp đồng thật qua {@code
 * ContractReadApi} (Phase 5 thay bản giả): hợp đồng Nháp, Chờ ký chưa đạt; Ghi nhận đã ký thì (b), (c), (e) đạt và Hoàn
 * tất tiếp nhận thành công, phát {@link AdmissionCompleted} với hợp đồng vừa ký.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_004_AdmissionWithRealContractIT extends ContractTestBase {

    private long application(long residentId) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        return applicationIdOf(residentId);
    }

    private List<String> notMet(long applicationId) throws Exception {
        user.set(office.user());
        JsonNode list = JSON.readTree(
                mvc.perform(get("/api/v1/admission-applications/" + applicationId + "/admission-conditions"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        return StreamSupport.stream(list.get("conditions").spliterator(), false)
                .filter(line -> "NOT_MET".equals(line.get("status").asString()))
                .map(line -> line.get("code").asString())
                .toList();
    }

    private void complete(long residentId) {
        statusService.completeAdmission(
                ResidentStatusService.target(residentId),
                new Reason("Hoàn tất thử"),
                JsonNodeFactory.instance.objectNode());
    }

    @Test
    void FR_004_theContractLifecycle_drivesConditionsBCE_andAdmissionCompletes() throws Exception {
        long residentId = residentWithRepresentative();
        long applicationId = application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);
        List<String> contractConditions = List.of("ADM_B_CONTRACT", "ADM_C_DEPOSIT", "ADM_E_CONTRACT_LEVEL");
        assertThat(notMet(applicationId)).containsAll(contractConditions);

        long id = createDraft(residentId);
        assertThat(notMet(applicationId)).as("Nháp").containsAll(contractConditions);
        send(id);
        assertThat(notMet(applicationId)).as("Chờ ký").containsAll(contractConditions);

        recordSignedRaw(id, signedBody(residentId, today())).andExpect(status().isOk());
        assertThat(notMet(applicationId)).as("Hiệu lực").isEmpty();

        complete(residentId);

        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(contractStatus(id)).isEqualTo("ACTIVE");
        assertThat(events.stream(AdmissionCompleted.class).filter(event -> event.residentId() == residentId))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.contractId()).isEqualTo(id);
                    assertThat(event.residenceType()).isEqualTo(ResidenceType.LONG_TERM_RESIDENTIAL);
                    assertThat(event.startDate()).isEqualTo(today());
                    assertThat(event.expectedEndDate()).isNull();
                });
    }

    @Test
    void FR_075_aContractStartingInTheFuture_isNotMetYet() throws Exception {
        long residentId = residentWithRepresentative();
        long applicationId = application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);
        ObjectNode body = body(residentId);
        body.put("startDate", today().plusDays(3).toString());
        activeContract(residentId, body);

        assertThat(notMet(applicationId)).containsExactly("ADM_B_CONTRACT");
    }

    @Test
    void BR_M02_22_aContractLevelDifferentFromTheAssessment_isNotMet_butTheContractIsStillCreated() throws Exception {
        long residentId = residentWithRepresentative();
        long applicationId = application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);
        ObjectNode body = body(residentId);
        body.put("careLevel", "REGULAR");
        body.putArray("services").add(line(regularCare));
        activeContract(residentId, body);

        assertThat(notMet(applicationId)).containsExactly("ADM_E_CONTRACT_LEVEL");
    }
}
