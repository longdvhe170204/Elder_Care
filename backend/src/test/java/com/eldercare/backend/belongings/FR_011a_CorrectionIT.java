package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.notification.api.NotificationLookupApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 013 T012 (US2; FR-011a, Q-160, research K-05; quickstart Q27, Q28): đính chính bản ghi bàn giao qua 000 (loại
 * {@code M12.BELONGING_HANDOVER}, không gắn tầng): không đổi loại; Hủy ghi nhận chỉ bản ghi gần nhất; sau đính chính tính
 * lại đồ; Hủy ghi nhận Tiếp nhận không có bản ghi sau làm đồ bị loại.
 */
class FR_011a_CorrectionIT extends HandoverTestBase {

    @Autowired
    NotificationLookupApi lookup;

    private ResultActions correct(String token, long handoverId, String body) throws Exception {
        return postJson(token, "/api/v1/corrections/M12.BELONGING_HANDOVER/" + handoverId, body);
    }

    private static String voiding() {
        return "{\"type\": \"VOID\", \"reason\": \"Ghi nhầm\"}";
    }

    private static String amend(String content) {
        return "{\"type\": \"AMEND_CONTENT\", \"correctedContent\": " + content + ", \"reason\": \"Ghi sai\"}";
    }

    @Test
    void voidingTheLatestRecord_recomputesBelongingFromThePreviousOne() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location);
        handover(tokenA, command("GIVE_FOR_USE", item(glasses, ""), "")).andExpect(status().isOk());
        int before = version(glasses);

        correct(tokenA, latestHandover(glasses), voiding()).andExpect(status().isCreated());

        assertThat(jdbc.queryForMap(
                        "SELECT status, holder_type, holder_id, storage_location_id, current_handover_id FROM belonging"
                                + " WHERE id = ?",
                        glasses))
                .containsEntry("status", "HELD")
                .containsEntry("holder_type", "STAFF")
                .containsEntry("holder_id", officeA.staffId())
                .containsEntry("storage_location_id", location)
                .containsEntry(
                        "current_handover_id",
                        jdbc.queryForObject(
                                "SELECT intake_handover_id FROM belonging WHERE id = ?", Long.class, glasses));
        assertThat(version(glasses)).isGreaterThan(before);
    }
}
