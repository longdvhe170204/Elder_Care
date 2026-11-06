package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 012 T062 (US9; FR-083, BR-M10-11; quickstart K1): kênh người cao tuổi có đủ năm phần của chính người đăng nhập: hôm
 * nay (lượt thăm, buổi hoạt động, lịch bán trú), thực đơn (011 chưa có API: báo chưa có dữ liệu), buổi hoạt động sắp tới
 * kèm đã có tên chưa, lượt thăm Đã duyệt kèm người thăm, đồ gửi đang giữ.
 */
class FR_083_ResidentChannelIT extends ResidentChannelTestBase {

    @Test
    void FR_083_todayHasVisitAndActivity() throws Exception {
        JsonNode today = getJsonAs(self(), "/api/v1/resident-channel/today");

        assertThat(today.get("date").asText()).isEqualTo(day.toString());
        assertThat(today.get("visits")).hasSize(1);
        assertThat(today.get("visits").get(0).get("startTime").asText()).isEqualTo("09:00");
        assertThat(names(today.get("visits").get(0).get("visitors"))).containsExactly("Lê M", "Lê Bé");
        assertThat(today.get("activities")).hasSize(1);
        assertThat(today.get("activities").get(0).get("name").asText()).isEqualTo("Hát karaoke");
        assertThat(today.get("dayCareSchedule")).isEmpty();
    }

    @Test
    void FR_083_upcomingSectionsOfSelf() throws Exception {
        JsonNode visits = getJsonAs(self(), "/api/v1/resident-channel/visits");
        assertThat(visits.get("available").asBoolean()).isTrue();
        assertThat(visits.get("items")).hasSize(1);
        assertThat(visits.get("items").get(0).get("date").asText()).isEqualTo(day.toString());

        JsonNode sessions = getJsonAs(self(), "/api/v1/resident-channel/activities");
        assertThat(sessions.get("available").asBoolean()).isTrue();
        assertThat(sessions.get("items").get(0).get("sessionId").asLong()).isEqualTo(sessionId);
        assertThat(sessions.get("items").get(0).get("registered").asBoolean()).isFalse();
        assertThat(sessions.get("items").get(0).get("facilityWide").asBoolean()).isTrue();

        JsonNode held = getJsonAs(self(), "/api/v1/resident-channel/belongings");
        assertThat(held.get("items").get(0).get("belongingId").asLong()).isEqualTo(belongingId);
        assertThat(held.get("items").get(0).get("name").asText()).isEqualTo("Đài radio");

        JsonNode menu = getJsonAs(self(), "/api/v1/resident-channel/menu");
        assertThat(menu.get("available").asBoolean()).isFalse();
        assertThat(menu.get("items")).isEmpty();
    }

    private static List<String> names(JsonNode array) {
        List<String> result = new ArrayList<>();
        array.forEach(n -> result.add(n.asText()));
        return result;
    }
}
