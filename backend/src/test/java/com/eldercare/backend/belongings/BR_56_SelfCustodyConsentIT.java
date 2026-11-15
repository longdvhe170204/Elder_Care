package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/**
 * 013 T008 (US1; BR-M12-06, Q-155; FR-005, FR-015a, FR-017b; quickstart Q3, Q11, Q12): Tiếp nhận và giao ngay; đồng ý
 * cho tự giữ một bản Hiệu lực mỗi đồ; rút đồng ý khi đồ đang sử dụng báo thu lại.
 */
class BR_56_SelfCustodyConsentIT extends IntakeTestBase {

    private String giveNow(long typeId, String amount, String giver, String extra) {
        return "{\"typeId\": " + typeId + ", \"description\": \"Đồ giao ngay\", " + amount + ","
                + " \"condition\": {\"levelId\": " + levelId("GOOD") + "}, \"giver\": " + giver + ","
                + " \"at\": \"2026-11-05T08:00:00+07:00\", \"giveForUseNow\": true, \"photoFileIds\": [" + photo()
                + "]" + extra + "}";
    }

    private String member(long id) {
        return "{\"type\": \"FAMILY_MEMBER\", \"id\": " + id + "}";
    }

    private long consentOf(long belongingId) {
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M12.SELF_CUSTODY_CONSENT' AND entity_id = ?"
                        + " ORDER BY id DESC LIMIT 1",
                Long.class,
                belongingId);
    }

    private String consentStatus(long consentId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, consentId);
    }

    @Test
    void BR_56_officeRecordsSignedConsent_onlyOneEffectivePerBelonging() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long representative = TestUsers.nextId();
        family.representative(representative, resident);
        long belonging = id(postJson(
                        office,
                        "/api/v1/residents/" + resident + "/belongings",
                        "{\"typeId\": " + typeId("JEWELRY") + ", \"description\": \"Nhẫn\", \"quantity\": 1,"
                                + " \"condition\": {\"levelId\": " + levelId("GOOD") + "}, \"giver\": "
                                + member(representative) + ", \"at\": \"2026-11-05T08:00:00+07:00\","
                                + " \"storageLocationId\": " + location() + ", \"photoFileIds\": [" + photo() + "]}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String consent = "{\"belongingId\": " + belonging + ", \"representativeId\": " + representative
                + ", \"signedFormFileId\": %d}";

        postJson(office, "/api/v1/self-custody-consents", consent.formatted(photo()))
                .andExpect(status().isCreated());
        postJson(office, "/api/v1/self-custody-consents", consent.formatted(photo()))
                .andExpect(status().isConflict());
        postJson(nurse, "/api/v1/self-custody-consents", consent.formatted(photo()))
                .andExpect(status().is4xxClientError());

        assertThat(consentStatus(consentOf(belonging))).isEqualTo("EFFECTIVE");
        assertThat(notices())
                .anySatisfy(n ->
                        assertThat(n.recipientGroups()).contains(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)));
    }
}
