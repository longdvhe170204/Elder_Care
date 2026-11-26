package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Q-451 (góp ý demo BF-10): biên nhận PDF của khoản nộp tiền đã xác nhận; Kế toán, Quản lý viện và người thân có quyền
 * xem chi phí tải được; giao dịch khác không có biên nhận.
 */
class PaymentReceiptIT extends BalanceTestBase {

    @Test
    void confirmedPayment_receiptForAccountantAndFamilyWithCosts_othersRefused() throws Exception {
        long resident = sentForSignature(TestUsers.nextId(), null);
        var created = postAs(
                        accountant,
                        "/api/v1/residents/" + resident + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":2500000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + time.now() + "\",\"note\":\"Nộp tại quầy\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long transaction = JsonMapper.builder()
                .build()
                .readTree(created)
                .at("/transactions/0/id")
                .asLong();
        String url = "/api/v1/balance-transactions/" + transaction + "/receipt";
        String portal = "/api/v1/family-portal/balance-transactions/" + transaction + "/receipt";

        byte[] pdf = getAs(accountant, url)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(DocumentReaders.pdfText(pdf))
                .contains("BIÊN NHẬN NỘP TIỀN")
                .contains("BN-" + String.format("%06d", transaction))
                .contains("2.500.000")
                .contains("Hai triệu năm trăm nghìn đồng")
                .contains("Lê Văn Hải")
                .contains("Tiền mặt")
                .contains("Chứng từ này không thay thế hóa đơn");
        getAs(manager, url).andExpect(status().isOk());
        getAs(office, url).andExpect(status().isForbidden());

        long withCosts = TestUsers.nextId();
        long withoutCosts = TestUsers.nextId();
        relationships.relate(withCosts, resident);
        relationships.relate(withoutCosts, resident);
        relationships.disable(withoutCosts, resident, FamilyRight.VIEW_COSTS);
        getAs(users.family(withCosts), portal).andExpect(status().isOk());
        getAs(users.family(withoutCosts), portal).andExpect(status().isForbidden());

        var adjustment = postAs(
                        accountant,
                        "/api/v1/balance-transactions",
                        "{\"type\":\"ADJUSTMENT\",\"residentId\":" + resident
                                + ",\"ledger\":\"BALANCE\",\"amount\":1000,\"reason\":\"Ghi bổ sung\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long other = JsonMapper.builder().build().readTree(adjustment).get("id").asLong();
        getAs(accountant, "/api/v1/balance-transactions/" + other + "/receipt")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECEIPT_NOT_AVAILABLE"));
        getAs(accountant, "/api/v1/balance-transactions/999999999/receipt").andExpect(status().isNotFound());
    }
}
