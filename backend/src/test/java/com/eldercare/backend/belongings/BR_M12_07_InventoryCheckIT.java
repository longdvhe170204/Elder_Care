package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.inventorycheck.service.InventoryCheckReminders;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 013 T023 (US5; BR-M12-07, FR-028a → FR-028c, Q-154, Q-306, Q-307; quickstart Q38 → Q41): phiếu kiểm kê đồ có giá trị:
 * một phiếu mỗi (vị trí, kỳ), gồm đúng đồ có giá trị Đang giữ, không gợi ý vị trí, hoàn thành sinh bản ghi cho lệch,
 * Đúng không sinh bản ghi, đồ đã đổi trạng thái bị loại, hủy khi mọi đồ đã rời, đồ không rõ chủ.
 */
class BR_M12_07_InventoryCheckIT extends HandoverTestBase {

    private static final String URL = "/api/v1/belonging-inventory-checks";

    @Autowired
    InventoryCheckReminders reminders;

    private long create(long location) throws Exception {
        String body = postJson(tokenA, URL, "{\"storageLocationId\": " + location + "}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long handoverRows(long belongingId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ?", Long.class, belongingId);
    }

    private static String line(long id, String result, String extra) {
        return "{\"belongingId\": " + id + ", \"result\": \"" + result + "\"" + extra + "}";
    }

    @Test
    void Q39_complete_createsRecordsOnlyForMismatches_andMatchedCreatesNone() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long ok = intake(resident, "PHONE", "\"quantity\": 1", location);
        long missing = intake(resident, "PHONE", "\"quantity\": 1", location);
        long worse = intake(resident, "PHONE", "\"quantity\": 1", location);
        long broken = intake(resident, "PHONE", "\"quantity\": 1", location);
        long id = create(location);
        long okRows = handoverRows(ok);
        long worseRows = handoverRows(worse);
        long unusable = levelId("PARTLY_DAMAGED");

        postJson(
                        tokenA,
                        URL + "/" + id + "/complete",
                        "{\"lines\": [" + line(ok, "MATCHED", "") + ","
                                + line(missing, "NOT_FOUND", "") + ","
                                + line(
                                        worse,
                                        "CONDITION_MISMATCH",
                                        ", \"newCondition\": {\"levelId\": " + unusable + "}")
                                + ","
                                + line(
                                        broken,
                                        "CONDITION_MISMATCH",
                                        ", \"newCondition\": {\"levelId\": " + unusable + "}, \"recordDamage\": true,"
                                                + " \"photoFileIds\": [" + photo() + "]")
                                + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(handoverRows(ok)).isEqualTo(okRows);
        assertThat(handoverRows(worse)).isEqualTo(worseRows);
        assertThat(statusOf(missing)).isEqualTo("LOST");
        assertThat(statusOf(broken)).isEqualTo("DAMAGED");
        assertThat(statusOf(worse)).isEqualTo("HELD");
        assertThat(jdbc.queryForObject(
                        "SELECT inventory_check_id FROM belonging_handover WHERE id = ?",
                        Long.class,
                        latestHandover(missing)))
                .isEqualTo(id);
        postJson(tokenA, URL + "/" + id + "/complete", "{\"lines\": []}").andExpect(status().isConflict());
    }
}
