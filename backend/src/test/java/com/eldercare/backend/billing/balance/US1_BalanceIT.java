package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/**
 * 017 T009, T045 (US1 #1 → #6; FR-001 → FR-004; quickstart Q1, Q1a, Q2): một sổ, một mã nộp tiền mỗi hồ sơ dù sự kiện
 * lặp hay hợp đồng trả về nháp; thông tin nộp tiền theo CFG-M11-10 (danh mục ngân hàng, kiểm khi lưu); Hành chính chỉ
 * thấy số dư, cọc, đặt cọc; Điều dưỡng 403; người thân không có quyền xem chi phí 403.
 */
class US1_BalanceIT extends BalanceTestBase {

    private static final String CFG = "/api/v1/parameters/CFG-M11-10";

    @Test
    void Q1_Q1a_Q2_oneLedgerPerProfile_paymentInstructionsFromCfg_andVisibilityByRole() throws Exception {
        long contract = TestUsers.nextId();
        long a = sentForSignature(contract, null);
        String code = paymentCode(a);
        changeContract(contract, a, ContractStatus.PENDING_SIGNATURE, ContractStatus.DRAFT);
        changeContract(contract, a, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE);
        changeContract(contract, a, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resident_balance WHERE resident_id = ?", Integer.class, a))
                .isOne();
        assertThat(paymentCode(a)).isEqualTo(code).matches("NCT[0-9A-HJKMNP-TV-Z]{6}[0-9A-HJKMNP-Z]");
        long b = sentForSignature(TestUsers.nextId(), 10_000_000L);
        assertThat(paymentCode(b)).isNotEqualTo(code);

        String url = "/api/v1/residents/" + a + "/balance";
        getAs(office, url)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.depositBalance").value(0))
                .andExpect(jsonPath("$.depositStatus").value("NOT_REQUIRED"))
                .andExpect(jsonPath("$.paymentCode").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields", hasItem("paymentCode")));
        getAs(nurse, url).andExpect(status().isForbidden());
        getAs(accountant, url)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentCode").value(code))
                .andExpect(jsonPath("$.status").value("NORMAL"))
                .andExpect(jsonPath("$.paymentInstructions").doesNotExist());
        getAs(accountant, "/api/v1/residents/" + b + "/balance")
                .andExpect(jsonPath("$.depositStatus").value("NOT_SATISFIED"));
        String typed =
                code.toLowerCase().substring(0, 5) + "-" + code.toLowerCase().substring(5);
        getAs(accountant, "/api/v1/balances/by-payment-code?code=" + typed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentId").value(a));
        char wrong = code.charAt(9) == '0' ? '1' : '0';
        getAs(accountant, "/api/v1/balances/by-payment-code?code=" + code.substring(0, 9) + wrong)
                .andExpect(status().isNotFound());

        long withCosts = TestUsers.nextId();
        long withoutCosts = TestUsers.nextId();
        relationships.relate(withCosts, a);
        relationships.relate(withoutCosts, a);
        relationships.disable(withoutCosts, a, FamilyRight.VIEW_COSTS);
        String portal = "/api/v1/family-portal/residents/" + a + "/balance";
        getAs(users.family(withoutCosts), portal).andExpect(status().isForbidden());

        getAs(manager, "/api/v1/banks")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.bin == '970436')].shortName").value("Vietcombank"));
        try {
            change("{\"bin\":\"999999\",\"accountNumber\":\"0011004000123\",\"accountHolder\":\"Viện A\"}")
                    .andExpect(status().isUnprocessableEntity());
            change("{\"bin\":\"970436\",\"accountNumber\":\"0011AB\",\"accountHolder\":\"Viện A\"}")
                    .andExpect(status().isUnprocessableEntity());
            change("{\"bin\":\"970436\",\"accountNumber\":\"0011004000123\",\"accountHolder\":\"Viện A\"}")
                    .andExpect(status().isOk());
            getAs(accountant, url)
                    .andExpect(jsonPath("$.paymentInstructions.bank").value("Vietcombank"))
                    .andExpect(jsonPath("$.paymentInstructions.accountNumber").value("0011004000123"))
                    .andExpect(jsonPath("$.paymentInstructions.vietQr", containsString("0006970436")))
                    .andExpect(jsonPath("$.paymentInstructions.vietQr", containsString(code)));
            getAs(users.family(withCosts), portal)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(0))
                    .andExpect(jsonPath("$.paymentInstructions.paymentCode").value(code))
                    .andExpect(jsonPath("$.transactions").isEmpty());
        } finally {
            as(manager);
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(CFG + "/change-value")
                            .with(org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.csrf())
                            .header("If-Match", etag())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"restoreDefault\":true,\"reason\":\"Trả lại sau test\"}"))
                    .andExpect(status().isOk());
        }
    }

    private String etag() throws Exception {
        as(manager);
        return mvc.perform(get(CFG)).andReturn().getResponse().getHeader("ETag");
    }

    private org.springframework.test.web.servlet.ResultActions change(String value) throws Exception {
        String ifMatch = etag();
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        CFG + "/change-value")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("If-Match", ifMatch)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"value\":" + value + ",\"reason\":\"Tài khoản thu của viện\"}"));
    }
}
