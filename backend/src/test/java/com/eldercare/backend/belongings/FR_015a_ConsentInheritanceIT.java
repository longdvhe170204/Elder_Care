package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 013-3 (A chốt 2026-11-05; FR-015a, FR-012, BR-M12-06): phần tách từ đồ có đồng ý cho tự giữ kế thừa đồng ý của đồ
 * gốc; đồng ý hết hiệu lực theo từng phần (phần sang Đã trả, Thất lạc, Hư hỏng, Đã xử lý); người đại diện rút thì hết
 * cho mọi phần.
 */
class FR_015a_ConsentInheritanceIT extends ReturnTestBase {

    private long resident;
    private long representative;
    private long location;
    private long cash;
    private long rootConsent;

    /** 2.000.000 tiền mặt Đang giữ, có đồng ý cho tự giữ do Hành chính ghi theo bản ký. */
    private void cashWithConsent() throws Exception {
        resident = resident("IN_RESIDENCE");
        representative = representative(resident, "012345");
        location = location();
        cash = intake(resident, "CASH", "\"cashAmount\": 2000000", location);
        rootConsent = id(postJson(
                        tokenA,
                        "/api/v1/self-custody-consents",
                        "{\"belongingId\": " + cash + ", \"representativeId\": " + representative
                                + ", \"signedFormFileId\": " + photo() + "}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    /** Giao sử dụng 500.000 trong 2.000.000; trả mã phần tách. */
    private long giveHalfMillionForUse() throws Exception {
        handover(
                        tokenA,
                        command(
                                "GIVE_FOR_USE",
                                item(cash, ", \"quantity\": 500000, \"photoFileIds\": [" + photo() + "]"),
                                ""))
                .andExpect(status().isOk());
        return jdbc.queryForObject("SELECT MAX(id) FROM belonging WHERE original_belonging_id = ?", Long.class, cash);
    }

    private List<String> consentStatuses(long belongingId) {
        return jdbc.queryForList(
                "SELECT status FROM simple_record WHERE kind = 'M12.SELF_CUSTODY_CONSENT' AND entity_id = ?"
                        + " ORDER BY id",
                String.class,
                belongingId);
    }

    private String returnTo(long belongingId, String quantity) {
        return returnBody(
                List.of("{\"belongingId\": " + belongingId + ", \"version\": " + version(belongingId)
                        + (quantity == null ? "" : ", \"quantity\": " + quantity) + ", \"condition\": {\"levelId\": "
                        + levelId("SIGNS_OF_USE") + "}, \"photoFileIds\": [" + photo() + "]}"),
                familyRecipient(representative, "Người đại diện", "CCCD", "012345"),
                "");
    }

    @Test
    void splitPart_inheritsConsent_soItCanBeGivenForUseAgainAfterTakeBack() throws Exception {
        cashWithConsent();
        long part = giveHalfMillionForUse();
        assertThat(consentStatuses(part)).containsExactly("EFFECTIVE");

        handover(
                        tokenA,
                        command(
                                "TAKE_BACK",
                                item(part, ", \"photoFileIds\": [" + photo() + "]"),
                                ", \"storageLocationId\": " + location))
                .andExpect(status().isOk());
        handover(tokenA, command("GIVE_FOR_USE", item(part, ", \"photoFileIds\": [" + photo() + "]"), ""))
                .andExpect(status().isOk());

        assertThat(statusOf(part)).isEqualTo("IN_USE");
        assertThat(consentStatuses(cash)).containsExactly("EFFECTIVE");
    }

    @Test
    void consentEndsPerPart_whenOnlyThatPartIsReturned() throws Exception {
        cashWithConsent();
        long part = giveHalfMillionForUse();

        giveBack(tokenA, returnTo(part, null)).andExpect(status().isCreated());
        giveBack(tokenA, returnTo(cash, "300000")).andExpect(status().isCreated());

        assertThat(consentStatuses(part)).containsExactly("EXPIRED");
        assertThat(consentStatuses(cash)).containsExactly("EFFECTIVE");
        long returnedPart =
                jdbc.queryForObject("SELECT MAX(id) FROM belonging WHERE original_belonging_id = ?", Long.class, cash);
        assertThat(statusOf(returnedPart)).isEqualTo("RETURNED");
        assertThat(consentStatuses(returnedPart)).doesNotContain("EFFECTIVE");
    }

    @Test
    void withdrawal_endsConsentOfEveryPart_andPartsInUseAreToldToBeTakenBack() throws Exception {
        cashWithConsent();
        long part = giveHalfMillionForUse();

        postJson(
                        tokenA,
                        "/api/v1/self-custody-consents/" + rootConsent + "/withdraw",
                        "{\"reason\": \"Người đại diện rút\", \"representativeId\": " + representative
                                + ", \"signedFormFileId\": " + photo() + "}")
                .andExpect(status().isOk());

        assertThat(consentStatuses(cash)).containsExactly("WITHDRAWN");
        assertThat(consentStatuses(part)).containsExactly("WITHDRAWN");
        assertThat(notices().stream()
                        .filter(n -> "SELF_CUSTODY_CONSENT_WITHDRAWN_IN_USE".equals(n.eventType())
                                && n.residentId().filter(id -> id == resident).isPresent())
                        .count())
                .isEqualTo(1);
    }
}
