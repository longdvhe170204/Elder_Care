package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.notification.api.NotificationLookupApi;
import java.time.Duration;
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

    @Test
    void FR_011a_voidingAnOlderRecord_isBlocked() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long intakeRecord = latestHandover(glasses);
        handover(tokenA, command("GIVE_FOR_USE", item(glasses, ""), "")).andExpect(status().isOk());

        correct(tokenA, intakeRecord, voiding())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_LATEST_HANDOVER"));
        assertThat(statusOf(glasses)).isEqualTo("IN_USE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M12.BELONGING_HANDOVER'"
                                + " AND original_record_id = ?",
                        Integer.class,
                        intakeRecord))
                .isZero();
    }

    @Test
    void FR_011a_voidingIntakeWithoutLaterRecords_excludesTheBelonging() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 3", location());

        correct(tokenA, latestHandover(clothes), voiding()).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT excluded FROM belonging WHERE id = ?", Boolean.class, clothes))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ?", Integer.class, clothes))
                .isEqualTo(1);
        getJson(tokenA, "/api/v1/residents/" + resident + "/belongings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void SC_001_amendingIntakeQuantity_recomputesSplitParts_totalFollowsCorrection() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 10", location());
        long intakeRecord = latestHandover(clothes);
        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(clothes, ", \"quantity\": 4"),
                                ", \"storageLocationId\": " + location() + ", \"reason\": \"Chia tủ\""))
                .andExpect(status().isOk());

        correct(tokenA, intakeRecord, amend("{\"quantity\": 9}")).andExpect(status().isCreated());

        assertThat(quantityOf(clothes)).isEqualByComparingTo("5");
        assertThat(intakeTotal(clothes)).isEqualByComparingTo("9");
    }

    @Test
    void FR_012_voidingTheSplitRecord_returnsQuantityToTheOriginal() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 10", location());
        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(clothes, ", \"quantity\": 4"),
                                ", \"storageLocationId\": " + location() + ", \"reason\": \"Chia tủ\""))
                .andExpect(status().isOk());
        long split = sameIntake(clothes).get(1);

        correct(tokenA, latestHandover(split), voiding()).andExpect(status().isCreated());

        assertThat(sameIntake(clothes)).containsExactly(clothes);
        assertThat(quantityOf(clothes)).isEqualByComparingTo("10");
    }

    @Test
    void amendingLocationOfTheLatestRecord_movesTheBelonging() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long right = location();

        correct(tokenA, latestHandover(clothes), amend("{\"storageLocationId\": " + right + "}"))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT storage_location_id FROM belonging WHERE id = ?", Long.class, clothes))
                .isEqualTo(right);
    }

    @Test
    void Q28_amendingTheHandoverType_isBlocked() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());

        correct(tokenA, latestHandover(clothes), amend("{\"kind\": \"GIVE_FOR_USE\"}"))
                .andExpect(status().isUnprocessableEntity());
        correct(tokenA, latestHandover(clothes), amend("{\"targetStatus\": \"IN_USE\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(statusOf(clothes)).isEqualTo("HELD");
    }

    @Test
    void Q27_voidingReportLost_restoresPreviousState_andNotifiesThoseWhoWereNotified() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        handover(tokenA, command("GIVE_FOR_USE", item(glasses, ""), "")).andExpect(status().isOk());
        handover(tokenA, command("REPORT_LOST", item(glasses, ""), ", \"reason\": \"Không thấy kính\""))
                .andExpect(status().isOk());
        long lostRecord = latestHandover(glasses);
        await().atMost(Duration.ofSeconds(30))
                .until(() -> !lookup.bySource("M12.BELONGING_HANDOVER", Long.toString(lostRecord))
                        .isEmpty());
        events.clear();

        correct(tokenA, lostRecord, voiding()).andExpect(status().isCreated());

        assertThat(statusOf(glasses)).isEqualTo("IN_USE");
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGING_HANDOVER_VOIDED");
            assertThat(n.recipientGroups()).isNotEmpty();
        });
    }
}
