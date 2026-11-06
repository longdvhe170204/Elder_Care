package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.family.api.events.VisitCheckOutReminder;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
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
    void FR_029_familyWithoutRegisterRight_isDenied_officeRegistersOnBehalf() throws Exception {
        register(family(xMember), null, MORNING_START, MORNING_END).andExpect(status().isForbidden());

        JsonNode visit =
                json(register(office, xMember, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        assertThat(visit.get("familyMemberId").asLong()).isEqualTo(xMember);
        assertThat(visit.get("status").asString()).isEqualTo("APPROVED");
    }

    @Test
    void Q22_outsideSlot_andTooLate_rejectedWithEveryReason_noVisitCreated() throws Exception {
        moveTo(at(day, LocalTime.of(11, 30)));
        register(family(mMember), null, LocalTime.of(12, 0), LocalTime.of(13, 0), "A", "B", "C")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.unmetConditions[*].code")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "OUTSIDE_VISIT_SLOT", "TOO_MANY_VISITORS", "OUTSIDE_REGISTRATION_WINDOW")));
        assertThat(visitsOfA()).isZero();
    }

    @Test
    void FR_030b_slotFull_isRejected() throws Exception {
        existingVisit(3);
        existingVisit(3);
        existingVisit(3);
        existingVisit(3);
        existingVisit(3);
        existingVisit(3);
        existingVisit(1);

        register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated());
        register(family(nMember), null, MORNING_START, MORNING_END, "Trần Lan")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("VISIT_SLOT_FULL"));
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

    @Test
    void FR_035_registrantCancelsBeforeStart_notAfter_officeNeedsReason() throws Exception {
        long first =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        visitCommand(family(mMember), first, "cancel", "{}").andExpect(status().isOk());
        assertThat(visitStatus(first)).isEqualTo("CANCELLED");
        JsonNode slots = getJsonAs(family(mMember), "/api/v1/visit-slots?date=" + day);
        assertThat(slots.get(0).get("seatsLeft").asInt()).isEqualTo(20);

        long second =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        visitCommand(family(nMember), second, "cancel", "{}").andExpect(status().isForbidden());
        visitCommand(office, second, "cancel", "{}").andExpect(status().isUnprocessableContent());
        moveTo(at(day, LocalTime.of(9, 5)));
        visitCommand(family(mMember), second, "cancel", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("START_TIME_PASSED"));
        assertThat(visitStatus(second)).isEqualTo("APPROVED");
    }

    @Test
    void Q26_checkIn_needsNamesWithinRegisteredCount_thenCheckOut() throws Exception {
        long visit = id(register(family(mMember), null, MORNING_START, MORNING_END, "Lê Hoa")
                .andExpect(status().isCreated()));
        moveTo(at(day, LocalTime.of(9, 10)));

        visitCommand(staffSession(Role.NURSING_STAFF), visit, "check-in", actual("Lê Hoa"))
                .andExpect(status().isForbidden());
        visitCommand(office, visit, "check-in", actual("Lê Hoa", "Ngô Bình"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_VISITORS"));
        visitCommand(
                        office,
                        visit,
                        "check-in",
                        "{\"actualVisitors\": [{\"fullName\": \"\", \"relationship\": \"Cháu\"}]}")
                .andExpect(status().isUnprocessableContent());

        visitCommand(office, visit, "check-in", actual("Ngô Bình")).andExpect(status().isOk());
        assertThat(visitStatus(visit)).isEqualTo("CHECKED_IN");

        moveTo(at(day, LocalTime.of(10, 20)));
        visitCommand(office, visit, "check-out", "{\"checkOutAt\": \"" + at(day, LocalTime.of(10, 30)) + "\"}")
                .andExpect(status().isUnprocessableContent());
        visitCommand(office, visit, "check-out", "{\"checkOutAt\": \"" + at(day, LocalTime.of(10, 15)) + "\"}")
                .andExpect(status().isOk());
        JsonNode read = visitOf(visit);
        assertThat(read.get("status").asString()).isEqualTo("CHECKED_OUT");
        assertThat(Instant.parse(read.get("checkedOutAt").asString())).isEqualTo(at(day, LocalTime.of(10, 15)));

        List<VisitTraceView> traces =
                emergencyFamily.visitsInRange(a, at(day, LocalTime.of(9, 0)), at(day, LocalTime.of(12, 0)));
        assertThat(traces).singleElement().satisfies(t -> {
            assertThat(t.visitId()).isEqualTo(visit);
            // Đồng hồ giả lập vẫn chạy sau khi đặt giờ
            assertThat(t.enteredAt()).isBetween(at(day, LocalTime.of(9, 10)), at(day, LocalTime.of(9, 11)));
            assertThat(t.people()).extracting(p -> p.fullName()).containsExactlyInAnyOrder("Lê M", "Ngô Bình");
            assertThat(t.people())
                    .filteredOn(p -> p.familyMemberId().isPresent())
                    .singleElement()
                    .satisfies(p -> assertThat(p.familyMemberId()).contains(mMember));
        });
    }

    @Test
    void FR_033_checkIn_rechecksPresence() throws Exception {
        long visit =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        moveTo(at(day, LocalTime.of(9, 10)));
        isolation.resident(a, List.of(zone()));

        visitCommand(office, visit, "check-in", actual())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ISOLATION_ACTIVE"));
        assertThat(visitStatus(visit)).isEqualTo("APPROVED");
    }

    @Test
    void Q27_noShowAfterSlot_andOneCheckOutReminder() throws Exception {
        long noShow =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        long staying =
                id(register(family(nMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        moveTo(at(day, LocalTime.of(9, 30)));
        visitCommand(office, staying, "check-in", actual()).andExpect(status().isOk());

        runMinuteTasks(at(day, LocalTime.of(10, 59)));
        assertThat(visitStatus(noShow)).isEqualTo("APPROVED");
        runMinuteTasks(at(day, LocalTime.of(11, 0)));
        assertThat(visitStatus(noShow)).isEqualTo("NO_SHOW");
        assertThat(visitStatus(staying)).isEqualTo("CHECKED_IN");

        runMinuteTasks(at(day, LocalTime.of(11, 29)));
        assertThat(events.stream(VisitCheckOutReminder.class).filter(e -> e.visitId() == staying))
                .isEmpty();
        runMinuteTasks(at(day, LocalTime.of(11, 30)));
        runMinuteTasks(at(day, LocalTime.of(11, 45)));
        assertThat(events.stream(VisitCheckOutReminder.class).filter(e -> e.visitId() == staying))
                .singleElement()
                .satisfies(e -> assertThat(e.notificationRequests()).isNotEmpty());
    }

    @Test
    void FR_046_familySeesOnlyOwnVisits_timetableListsTheDay() throws Exception {
        long mine =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        id(register(family(nMember), null, AFTERNOON_START, AFTERNOON_END).andExpect(status().isCreated()));

        JsonNode own = getJsonAs(family(mMember), "/api/v1/visits?residentId=" + a + "&date=" + day);
        assertThat(own).hasSize(1);
        assertThat(own.get(0).get("id").asLong()).isEqualTo(mine);
        assertThat(getJson("/api/v1/visits?residentId=" + a + "&date=" + day)).hasSize(2);
        mvc.perform(get("/api/v1/visits?residentId=" + a + "&date=" + day)
                        .header("Authorization", staffSession(Role.KITCHEN_STAFF)))
                .andExpect(status().isForbidden());

        TimetableSourcePort timetable = timetables.stream()
                .filter(t -> t.getClass().getPackageName().startsWith("com.eldercare.backend.family"))
                .findFirst()
                .orElseThrow();
        assertThat(timetable.onDate(a, day)).hasSize(2);
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
