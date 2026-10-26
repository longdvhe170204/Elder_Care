package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m02.api.events.EndingExceptionApproved;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T057, T106 (US7; FR-064, Q-379, Q-313; loại yêu cầu {@code M02.ENDING_EXCEPTION}): Hành chính lập ngoại lệ cho điều
 * kiện (a) → (d), (h); Kế toán chỉ với ý tiền của (h); Quản lý viện duyệt; ngoại lệ miễn đúng mã, không bao giờ miễn
 * bàn giao (e) hay ý "Kế toán đã xác nhận quyết toán".
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingExceptionIT extends EndingTestBase {

    @Test
    void FR_064_anApprovedException_marksTheLineMetByException_andLetsTheCommandPass() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        otherUnmet(D_ALERTS, true);
        assertThat(unmetCodes(endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0))))
                .containsExactly(D_ALERTS);

        long requestId = approvedException(ready.endingId(), D_ALERTS);

        JsonNode list = conditions(ready.endingId());
        assertThat(line(list, D_ALERTS).get("status").asString()).isEqualTo("MET_BY_EXCEPTION");
        assertThat(line(list, D_ALERTS)
                        .get("exception")
                        .get("approvalRequestId")
                        .asLong())
                .isEqualTo(requestId);
        assertThat(jdbc.queryForMap(
                                "SELECT condition_code, ending_id, resident_id FROM ending_condition_exception"
                                        + " WHERE approval_request_id = ?",
                                requestId)
                        .get("condition_code"))
                .isEqualTo(D_ALERTS);
        assertThat(events.stream(EndingExceptionApproved.class)
                        .filter(e -> e.conditionCode().equals(D_ALERTS)))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.residencyEndingId()).isEqualTo(ready.endingId());
                    assertThat(e.residentId()).isEqualTo(ready.residentId());
                });

        endResidency(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));

        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
        assertThat(jdbc.queryForObject(
                        "SELECT condition_waiver FROM resident_status_history WHERE resident_id = ?"
                                + " AND command = 'END_RESIDENCY'",
                        String.class,
                        ready.residentId()))
                .contains(D_ALERTS)
                .contains(String.valueOf(requestId));
    }

    @Test
    void FR_064_anExceptionWaivesOnlyItsOwnCondition() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        otherUnmet(B_BELONGINGS, true);
        otherUnmet(D_ALERTS, true);
        approvedException(ready.endingId(), D_ALERTS);

        BusinessException blocked =
                endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));

        assertThat(unmetCodes(blocked)).containsExactly(B_BELONGINGS);
    }

    @Test
    void FR_064_handoverAndTheSettlementConfirmation_haveNoException() throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, d0.plusDays(3));

        requestExceptionRaw(office, endingId, "END_E_HANDOVER").andExpect(status().isUnprocessableEntity());
        requestExceptionRaw(office, endingId, H_CONFIRMATION).andExpect(status().isUnprocessableEntity());
        requestExceptionRaw(office, endingId, "END_X_UNKNOWN").andExpect(status().isUnprocessableEntity());
        requestExceptionRaw(office, endingId, "END_A_COSTS").andExpect(status().isCreated());
    }

    @Test
    void FR_064_theAccountant_requestsOnlyTheSettlementException() throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, d0.plusDays(3));

        requestExceptionRaw(accountant, endingId, B_BELONGINGS).andExpect(status().isForbidden());
        requestExceptionRaw(accountant, endingId, "END_A_COSTS").andExpect(status().isForbidden());
        requestExceptionRaw(accountant, endingId, H_SETTLEMENT).andExpect(status().isCreated());
        requestExceptionRaw(doctor, endingId, D_ALERTS).andExpect(status().isForbidden());
    }

    @Test
    void FR_064_theAccountant_canReadTheConditions_butCannotRunTheEndingCommands() throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, d0.plusDays(3));

        user.set(accountant.user());
        mvc.perform(get("/api/v1/residency-endings/" + endingId + "/conditions"))
                .andExpect(status().isOk());
        createEndingRaw(accountant, endingBody(residentId, "DISCHARGE", d0.plusDays(4)))
                .andExpect(status().isForbidden());
        cancelRaw(accountant, endingId, "Không có quyền").andExpect(status().isForbidden());
        handoverRaw(accountant, endingId, handoverBody()).andExpect(status().isForbidden());
        assertThat(endingStatus(endingId)).isEqualTo("PREPARING");
    }

    @Test
    void FR_064_onlyAPreparingEnding_takesAnException() throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, d0.plusDays(3));
        cancelRaw(office, endingId, "Đổi ý").andExpect(status().isOk());

        requestExceptionRaw(office, endingId, D_ALERTS)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENCY_ENDING_NOT_PREPARING"));
    }

    @Test
    void FR_064_approvingAfterTheEndingWasCancelled_isRejected_andAddsNoException() throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, d0.plusDays(3));
        long requestId = JSON.readTree(requestExceptionRaw(office, endingId, D_ALERTS)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("approvalRequestId")
                .asLong();
        cancelRaw(office, endingId, "Đổi ý").andExpect(status().isOk());

        user.set(manager.user());
        String etag = mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"ok\"}"))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ending_condition_exception WHERE approval_request_id = ?",
                        Integer.class,
                        requestId))
                .isZero();
    }

    @Test
    void FR_064_settlementException_stillNeedsTheAccountantsConfirmation() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        otherUnmet(H_SETTLEMENT, true);
        otherUnmet(H_CONFIRMATION, false);
        approvedException(ready.endingId(), H_SETTLEMENT);

        BusinessException blocked =
                endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));
        JsonNode list = conditions(ready.endingId());

        assertThat(unmetCodes(blocked)).containsExactly(H_CONFIRMATION);
        assertThat(line(list, H_SETTLEMENT).get("status").asString()).isEqualTo("NOT_MET");
        assertThat(line(list, H_SETTLEMENT).get("basis").asString()).contains(H_CONFIRMATION);

        otherModules.reset();
        otherModules.interestedIn(StatusCommand.END_RESIDENCY);
        otherUnmet(H_SETTLEMENT, true);

        assertThat(line(conditions(ready.endingId()), H_SETTLEMENT)
                        .get("status")
                        .asString())
                .isEqualTo("MET_BY_EXCEPTION");
        endResidency(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));
        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
    }

    @Test
    void FR_064_accountantsSettlementException_isApprovedByTheManagerLikeAnyOther() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        otherUnmet(H_SETTLEMENT, true);
        long requestId = JSON.readTree(requestExceptionRaw(accountant, ready.endingId(), H_SETTLEMENT)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("approvalRequestId")
                .asLong();

        approve(requestId);

        assertThat(line(conditions(ready.endingId()), H_SETTLEMENT)
                        .get("status")
                        .asString())
                .isEqualTo("MET_BY_EXCEPTION");
    }
}
