package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Gender;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T020 (US2; FR-008, FR-009, FR-010, BR-M02-10, BR-M02-19): Bộ lập lịch tính lại điểm mỗi ngày cho hồ sơ Đang chờ
 * và Đã liên hệ; lịch sử chỉ thêm khi điểm đổi; điểm cũng tính lại ngay khi mức chăm sóc hiện hành hoặc bảng CFG-M02-08
 * đổi (US2 #2).
 */
@AutoConfigureMockMvc
@WithMockUser
class DailyScoreIT extends WaitingListTestBase {

    private int historyRows(long entryId, String cause) throws Exception {
        int count = 0;
        for (JsonNode row : scoreHistory(entryId)) {
            if (cause.equals(row.get("cause").asString())) {
                count++;
            }
        }
        return count;
    }

    /** Hồ sơ chờ lập ở 12:00 hôm nay, đã chờ 15 ngày: điểm 9 (US2 #1). */
    private long entryWaiting15Days() throws Exception {
        setScoreTable(SCORE_TABLE);
        noonOf(today());
        return newEntry(Gender.FEMALE, today().minusDays(15), "SPECIAL", "LIVING_ALONE")
                .get("id")
                .asLong();
    }

    @Test
    void US2_2_sevenDaysLater_dailyJobAddsOnePoint_andRecordsOneHistoryRow() throws Exception {
        long id = entryWaiting15Days();
        assertThat(entry(id).get("score").asInt()).isEqualTo(9);
        LocalDate today = today();

        noonOf(today.plusDays(7));
        dailyRunner.catchUp();

        assertThat(entry(id).get("score").asInt()).isEqualTo(10);
        assertThat(entry(id).get("scoreBreakdown").get("waitingTime").asInt()).isEqualTo(3);
        JsonNode history = scoreHistory(id);
        JsonNode last = history.get(history.size() - 1);
        assertThat(last.get("cause").asString()).isEqualTo("DAILY");
        assertThat(last.get("oldScore").asInt()).isEqualTo(9);
        assertThat(last.get("newScore").asInt()).isEqualTo(10);
        assertThat(historyRows(id, "DAILY")).isEqualTo(1);
    }

    @Test
    void FR_010_daysWithoutAChange_addNoHistoryRow_andReRunningIsHarmless() throws Exception {
        long id = entryWaiting15Days();
        LocalDate today = today();

        noonOf(today.plusDays(7));
        dailyRunner.catchUp();
        noonOf(today.plusDays(8));
        dailyRunner.catchUp();
        dailyRunner.catchUp();
        noonOf(today.plusDays(9));
        dailyRunner.catchUp();

        assertThat(entry(id).get("score").asInt()).isEqualTo(10);
        assertThat(historyRows(id, "DAILY")).as("chỉ thêm khi điểm đổi").isEqualTo(1);
    }

    @Test
    void BR_M02_19_catchUpAfterAGap_scoresOnlyTheLatestDay() throws Exception {
        long id = entryWaiting15Days();
        LocalDate today = today();
        noonOf(today);
        dailyRunner.catchUp();

        noonOf(today.plusDays(15));
        dailyRunner.catchUp();

        // Chờ 30 ngày: 4 chu kỳ trọn vẹn. Ngày lỡ ghi "bỏ qua" (research T-11) nên lịch sử có một dòng, không phải hai
        assertThat(entry(id).get("score").asInt()).isEqualTo(5 + 2 + 4);
        assertThat(historyRows(id, "DAILY")).isEqualTo(1);
        assertThat(scoreHistory(id).get(1).get("oldScore").asInt()).isEqualTo(9);
        assertThat(jdbc.queryForObject(
                        "SELECT result FROM daily_run WHERE task = 'CALCULATE_SCORES' AND `date` = ?",
                        String.class,
                        today.plusDays(3)))
                .isEqualTo("SKIPPED");
    }

    @Test
    void FR_010_contactedEntryIsRescoredToo() throws Exception {
        long id = entryWaiting15Days();
        jdbc.update("UPDATE waiting_list_entry SET status = 'CONTACTED', held_bed_id = ? WHERE id = ?", 777_001L, id);

        noonOf(today().plusDays(7));
        dailyRunner.catchUp();

        assertThat(entry(id).get("score").asInt()).isEqualTo(10);
        assertThat(entry(id).get("status").asString()).isEqualTo("CONTACTED");
    }

    @Test
    void FR_010_closedEntryIsNotRescored() throws Exception {
        long id = entryWaiting15Days();
        jdbc.update("UPDATE waiting_list_entry SET status = 'DECLINED' WHERE id = ?", id);

        noonOf(today().plusDays(14));
        dailyRunner.catchUp();

        assertThat(entry(id).get("score").asInt()).isEqualTo(9);
    }

    @Test
    void FR_008_careLevelChange_isTakenImmediately() throws Exception {
        setScoreTable(SCORE_TABLE);
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");
        long id = createEntry(applicationId).get("id").asLong();
        assertThat(entry(id).get("careLevel").asString()).isEqualTo("BASIC");
        assertThat(entry(id).get("notAssessed").asBoolean()).isTrue();

        confirmedAssessment(residentId, CareLevel.SPECIAL);

        // Luồng chờ không có người dùng đăng nhập của test nên đọc thẳng cơ sở dữ liệu
        await().atMost(Duration.ofSeconds(10)).until(() -> "SPECIAL".equals(storedCareLevel(id)));
        JsonNode after = entry(id);
        assertThat(after.get("notAssessed").asBoolean()).isFalse();
        assertThat(after.get("score").asInt()).isEqualTo(5);
        assertThat(historyRows(id, "CARE_LEVEL_CHANGED")).isEqualTo(1);
    }

    @Test
    void US2_2_changingTheScoreTable_rescoresEveryOpenEntryAtOnce() throws Exception {
        long id = entryWaiting15Days();
        assertThat(entry(id).get("score").asInt()).isEqualTo(9);

        setScoreTable(SCORE_TABLE.replace("\"SPECIAL\":5", "\"SPECIAL\":8"));

        await().atMost(Duration.ofSeconds(10)).until(() -> storedScore(id) == 12);
        assertThat(historyRows(id, "SCORE_TABLE_CHANGED")).isEqualTo(1);
    }

    private String storedCareLevel(long entryId) {
        return jdbc.queryForObject("SELECT care_level FROM waiting_list_entry WHERE id = ?", String.class, entryId);
    }

    private int storedScore(long entryId) {
        return jdbc.queryForObject("SELECT current_score FROM waiting_list_entry WHERE id = ?", Integer.class, entryId);
    }
}
