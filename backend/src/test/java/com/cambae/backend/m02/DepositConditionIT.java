package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.DepositStatus;
import com.cambae.backend.m02.api.dto.DepositStatusView;
import com.cambae.backend.m02.internal.application.service.AdmissionContractConditions;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T039 (US4; FR-030, FR-031, FR-033): điều kiện (c) đọc trạng thái đặt cọc của 017 qua {@code DepositStatusPort}
 * (bản giả) và {@code GET /contracts/{id}/deposit} trả khoản cần đặt cọc kèm trạng thái, căn cứ.
 */
@AutoConfigureMockMvc
@WithMockUser
class DepositConditionIT extends ContractTestBase {

    @Autowired
    private AdmissionContractConditions conditions;

    private ObjectNode withDeposit(long residentId, long amount) {
        ObjectNode body = body(residentId);
        body.put("noDepositRequired", false);
        body.put("depositAmount", amount);
        return body;
    }

    private List<String> unmet(long residentId) {
        return conditions.unmet(residentId, Instant.now()).stream()
                .map(condition -> condition.code())
                .toList();
    }

    @Test
    void FR_033_depositCondition_followsTheStatusFrom017() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.BASIC);
        long id = activeContract(residentId, withDeposit(residentId, 15_000_000));

        deposits.status(id, DepositStatus.NOT_SATISFIED);
        assertThat(unmet(residentId)).containsExactly(AdmissionContractConditions.DEPOSIT);

        deposits.status(id, DepositStatus.SATISFIED);
        assertThat(unmet(residentId)).isEmpty();

        deposits.status(id, DepositStatus.NOT_REQUIRED);
        assertThat(unmet(residentId)).isEmpty();
    }

    @Test
    void FR_030_deposit_returnsTheRequiredAmountAndNotSatisfiedStatus() throws Exception {
        long residentId = residentWithRepresentative();
        long id = create(withDeposit(residentId, 15_000_000))
                .get("contract")
                .get("id")
                .asLong();
        deposits.status(id, DepositStatus.NOT_SATISFIED);

        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + id + "/deposit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notRequired").value(false))
                .andExpect(jsonPath("$.requiredAmount").value(15_000_000))
                .andExpect(jsonPath("$.status").value("NOT_SATISFIED"))
                .andExpect(jsonPath("$.basis").doesNotExist());
    }

    @Test
    void FR_031_deposit_satisfied_carriesTheBasis() throws Exception {
        long residentId = residentWithRepresentative();
        long id = create(withDeposit(residentId, 15_000_000))
                .get("contract")
                .get("id")
                .asLong();
        var accountant = users.staff(Role.ACCOUNTANT);
        Instant at = Instant.parse("2026-10-20T03:00:00Z");
        deposits.status(id, new DepositStatusView(DepositStatus.SATISFIED, at, 77L, accountant.accountId()));

        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + id + "/deposit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SATISFIED"))
                .andExpect(jsonPath("$.basis.at").value("2026-10-20T03:00:00Z"))
                .andExpect(jsonPath("$.basis.transactionId").value(77))
                .andExpect(jsonPath("$.basis.accountant").isNotEmpty());
    }

    @Test
    void FR_030_deposit_notRequired_hasNoAmount() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);
        deposits.status(id, DepositStatus.NOT_REQUIRED);

        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + id + "/deposit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notRequired").value(true))
                .andExpect(jsonPath("$.requiredAmount").value(0))
                .andExpect(jsonPath("$.status").value("NOT_REQUIRED"));
    }

    @Test
    void FR_030_deposit_ofAnUnknownContract_is404() throws Exception {
        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/999999999/deposit")).andExpect(status().isNotFound());
    }
}
