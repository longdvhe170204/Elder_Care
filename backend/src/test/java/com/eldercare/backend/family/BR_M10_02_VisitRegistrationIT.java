package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * 012 T022 ({@code BR_M10_02_DangKyThamIT}; Q21 → Q23, Q26 → Q28; FR-029 → FR-035, FR-038, FR-039, FR-067, BR-M10-02):
 * đăng ký đạt thì Đã duyệt ngay và giữ chỗ; không đạt thì 422 kèm mọi lý do, không tạo lượt; Hành chính đăng ký thay,
 * ghi vào (kiểm lại (c), (d), người thực tế), ghi ra; hủy trước giờ bắt đầu; hết khung chưa vào thành Không đến; quá giờ
 * chưa ra nhắc Hành chính một lần; 007 đọc lượt đã vào cho truy vết.
 */
class BR_M10_02_VisitRegistrationIT extends VisitTestBase {

    @Autowired
    EmergencyFamilyPort emergencyFamily;

    @Autowired
    List<TimetableSourcePort> timetables;

    @Test
    void Q21_portalRegistration_isApprovedAtOnce_andHoldsSeats() throws Exception {
        JsonNode visit = json(register(family(mMember), null, MORNING_START, MORNING_END, "Lê Hoa", "Lê Nam")
                .andExpect(status().isCreated()));

        assertThat(visit.get("status").asString()).isEqualTo("APPROVED");
        assertThat(visit.get("visitorCount").asInt()).isEqualTo(3);
        assertThat(visit.get("familyMemberId").asLong()).isEqualTo(mMember);
        JsonNode slots = getJsonAs(family(mMember), "/api/v1/visit-slots?date=" + day);
        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).get("startTime").asString()).isEqualTo("09:00");
        assertThat(slots.get(0).get("seatsLeft").asInt()).isEqualTo(17);
        assertThat(slots.get(1).get("seatsLeft").asInt()).isEqualTo(20);
    }

    @Test
    void Q23_isolation_andTemporaryAbsence_rejected() throws Exception {
        isolation.resident(a, List.of(zone()));
        register(family(mMember), null, MORNING_START, MORNING_END)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("ISOLATION_ACTIVE"));

        isolation.resident(a, List.of());
        jdbc.update("UPDATE resident SET status = 'TEMPORARILY_ABSENT' WHERE id = ?", a);
        register(family(mMember), null, MORNING_START, MORNING_END)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("RESIDENT_NOT_PRESENT"));
        assertThat(visitsOfA()).isZero();
    }

    private static String actual(String... names) {
        StringBuilder people = new StringBuilder();
        for (String name : names) {
            if (!people.isEmpty()) {
                people.append(", ");
            }
            people.append("{\"fullName\": \"").append(name).append("\", \"relationship\": \"Cháu\"}");
        }
        return "{\"actualVisitors\": [" + people + "]}";
    }

    private IsolationZoneView zone() {
        return new IsolationZoneView(900, Set.of(), Set.of(), time.now().minus(Duration.ofHours(1)), null);
    }

    private int visitsOfA() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = 'M10.VISIT' AND resident_id = ?", Integer.class, a);
    }

    private JsonNode visitOf(long visitId) throws Exception {
        for (JsonNode v : getJson("/api/v1/visits?residentId=" + a + "&date=" + day)) {
            if (v.get("id").asLong() == visitId) {
                return v;
            }
        }
        throw new AssertionError("Không thấy lượt " + visitId);
    }
}
