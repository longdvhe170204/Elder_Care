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
    void Q38_checkHoldsExactlyTheValuableHeldItems_oncePerLocationAndPeriod() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        intake(resident, "PHONE", "\"quantity\": 1", location);
        intake(resident, "JEWELRY", "\"quantity\": 1", location);
        intake(resident, "CLOTHING", "\"quantity\": 1", location);

        long id = create(location);

        getJson(tokenA, URL + "/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CHECK"))
                .andExpect(jsonPath("$.content.lines.length()").value(2));
        postJson(tokenA, URL, "{\"storageLocationId\": " + location + "}").andExpect(status().isConflict());
        postJson(tokenA, URL, "{\"storageLocationId\": " + location() + "}").andExpect(status().isConflict());
        postJson(nurse, URL, "{\"storageLocationId\": " + location + "}").andExpect(status().isForbidden());
    }

    @Test
    void Q38_systemNeverCreatesChecksOrSuggestsLocations() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        intake(resident, "PHONE", "\"quantity\": 1", location);
        long before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = 'M12.INVENTORY_CHECK'", Long.class);

        reminders.remind();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind = 'M12.INVENTORY_CHECK'", Long.class))
                .isEqualTo(before);
        getJson(manager, URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations[?(@.storageLocationId == " + location + ")].valuableCount")
                        .value(org.hamcrest.Matchers.contains(1)));
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

    @Test
    void Q39_completeNeedsAResultForEveryItemStillThere_andFailureLeavesNothingBehind() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long first = intake(resident, "PHONE", "\"quantity\": 1", location);
        long second = intake(resident, "PHONE", "\"quantity\": 1", location);
        long id = create(location);

        postJson(tokenA, URL + "/" + id + "/complete", "{\"lines\": [" + line(first, "NOT_FOUND", "") + "]}")
                .andExpect(status().isUnprocessableContent());

        assertThat(statusOf(first)).isEqualTo("HELD");
        assertThat(statusOf(second)).isEqualTo("HELD");
        getJson(tokenA, URL + "/" + id).andExpect(jsonPath("$.status").value("WAITING_CHECK"));
    }

    @Test
    void Q40_itemChangedAfterCreationIsDropped_andCancelNeedsEveryItemGone() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long gone = intake(resident, "PHONE", "\"quantity\": 1", location);
        long stays = intake(resident, "PHONE", "\"quantity\": 1", location);
        long id = create(location);

        postJson(tokenA, URL + "/" + id + "/cancel", "{\"reason\": \"Thử\"}").andExpect(status().isConflict());
        jdbc.update(
                "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?", gone);
        postJson(tokenA, URL + "/" + id + "/complete", "{\"lines\": [" + line(stays, "MATCHED", "") + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.lines[0].result").value("DROPPED"));

        long other = location();
        long onlyOne = intake(resident, "PHONE", "\"quantity\": 1", other);
        long cancelId = create(other);
        jdbc.update(
                "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?", onlyOne);
        postJson(tokenA, URL + "/" + cancelId + "/cancel", "{}").andExpect(status().isUnprocessableContent());
        postJson(tokenA, URL + "/" + cancelId + "/cancel", "{\"reason\": \"Đã trả hết\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void Q41_foundLostItem_andUnknownOwnerItem_areHandled() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location);
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        handover(tokenA, command("REPORT_LOST", item(clothes, ""), ", \"reason\": \"Mất\""))
                .andExpect(status().isOk());
        long id = create(location);

        postJson(
                        tokenA,
                        URL + "/" + id + "/complete",
                        "{\"lines\": [" + line(phone, "MATCHED", "")
                                + "], \"unknownOwnerItems\": [{\"description\": \"Đồng hồ\"}]}")
                .andExpect(status().isUnprocessableContent());
        postJson(
                        tokenA,
                        URL + "/" + id + "/complete",
                        "{\"lines\": [" + line(phone, "MATCHED", "") + "], \"found\": [" + clothes + "],"
                                + " \"unknownOwnerItems\": [{\"description\": \"Đồng hồ\", \"photoFileIds\": ["
                                + photo() + "]}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.unknownOwnerItems.length()").value(1));

        assertThat(statusOf(clothes)).isEqualTo("HELD");
        assertThat(jdbc.queryForObject(
                        "SELECT kind FROM belonging_handover WHERE id = ?", String.class, latestHandover(clothes)))
                .isEqualTo("FOUND");
        assertThat(notices().stream().anyMatch(n -> "INVENTORY_UNKNOWN_OWNER_ITEMS".equals(n.eventType())))
                .isTrue();
    }
}
