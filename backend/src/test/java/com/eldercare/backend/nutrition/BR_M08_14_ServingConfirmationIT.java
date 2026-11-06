package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.common.offline.SyncResult;
import com.eldercare.backend.incident.api.events.AlertRequested;
import com.eldercare.backend.nutrition.internal.serving.service.ServingOfflineHandler;
import com.eldercare.backend.nutrition.internal.serving.service.ServingService.IncidentData;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 011 T032 (US6, FR-049, FR-051, BR-M08-14; quickstart Q38 → Q39, Q41): xác nhận phục vụ đúng người, sai người (không sự cố),
 * xung đột dị ứng phát sinh sau chốt (cảnh báo 007 còn dù lệnh bị chặn; đã ăn → sự cố 007; chưa ăn → không sự cố), xung đột
 * hạn chế, người ngoài tầng/khu, ghi ngoại tuyến kiểm lại khi đồng bộ.
 */
class BR_M08_14_ServingConfirmationIT extends ServingTestBase {

    @Autowired
    ServingOfflineHandler offline;

    @Test
    void BR_M08_14_rightPersonRecorded_wrongPersonBlockedWithoutIncident_outsiderForbidden() throws Exception {
        publishedMenu(day.minusDays(2), plain);
        long e = softResident(room2);
        long g = softResident(room2);
        closeLunchAndServe();
        long forE = specialOf(e);
        long forG = specialOf(g);
        int incidentsBefore = incidentCommands.recorded().size();

        as(serverOf(floor3));
        confirm(forE, e, null).andExpect(status().isNotFound());

        as(serverOf(floor2));
        confirm(forE, g, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PORTION_WRONG_RESIDENT"));
        assertThat(confirmations(forE)).isZero();
        assertThat(incidentCommands.recorded()).hasSize(incidentsBefore);
        assertThat(events.stream(AlertRequested.class)).isEmpty();

        confirm(forE, e, null).andExpect(status().isCreated());
        confirm(forG, g, null).andExpect(status().isCreated());
        assertThat(result(forE)).isEqualTo("CORRECT_PORTION");
        assertThat(result(forG)).isEqualTo("CORRECT_PORTION");
        send(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/special-portions/" + forE + "/confirm-serving"),
                        "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void BR_M08_14_allergyAfterClosing_blocksAndAlerts_eatenRecordsIncident() throws Exception {
        long tom = f.allergen();
        long shrimp = f.activeDish(List.of(tom), List.of(defaultDiet));
        publishedMenu(day.minusDays(2), shrimp);
        long a = softResident(room2);
        closeLunchAndServe();
        long special = specialOf(a);
        allergy(a, tom);
        int incidentsBefore = incidentCommands.recorded().size();

        as(serverOf(floor2));
        confirm(special, a, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PORTION_HAS_ALLERGEN"));
        assertThat(confirmations(special)).isZero();
        assertThat(events.stream(AlertRequested.class)).singleElement().satisfies(alert -> {
            assertThat(alert.residentId()).isEqualTo(a);
            assertThat(alert.type()).isEqualTo("ALLERGEN_PORTION_SERVED");
            assertThat(alert.level()).isEqualTo("MEDIUM");
            assertThat(alert.sourceType()).isEqualTo("SPECIAL_PORTION");
            assertThat(alert.sourceId()).isEqualTo(special);
        });

        confirm(special, a, "\"hasEaten\":true").andExpect(status().isUnprocessableEntity());
        assertThat(confirmations(special)).isZero();
        confirm(
                        special,
                        a,
                        "\"hasEaten\":true,\"incident\":{\"severity\":\"MEDIUM\","
                                + "\"symptoms\":\"Nổi mề đay\",\"treatment\":\"Uống thuốc kháng histamin\"}")
                .andExpect(status().isCreated());

        assertThat(incidentCommands.recorded()).hasSize(incidentsBefore + 1);
        assertThat(incidentCommands.recorded().getLast()).satisfies(c -> {
            assertThat(c.residentId()).isEqualTo(a);
            assertThat(c.source()).isEqualTo("EATING");
            assertThat(c.description()).isEqualTo("Nổi mề đay");
            assertThat(c.level()).isEqualTo("MEDIUM");
            assertThat(c.initialResponse()).isEqualTo("Uống thuốc kháng histamin");
            assertThat(c.floorAreaId()).isEqualTo(floor2);
        });
        assertThat(result(special)).isEqualTo("BLOCKED_CONFLICT_EATEN");
        assertThat(jdbc.queryForObject(
                        "SELECT content->>'$.incidentId' FROM record_event WHERE kind = 'M08.SERVING_CONFIRMATION'"
                                + " AND entity_id = ?",
                        String.class,
                        special))
                .isNotNull();
    }

    @Test
    void BR_M08_14_allergyAfterClosing_notEaten_noIncident() throws Exception {
        long tom = f.allergen();
        long shrimp = f.activeDish(List.of(tom), List.of(defaultDiet));
        publishedMenu(day.minusDays(2), shrimp);
        long a = softResident(room2);
        closeLunchAndServe();
        long special = specialOf(a);
        allergy(a, tom);
        int incidentsBefore = incidentCommands.recorded().size();

        as(serverOf(floor2));
        confirm(special, a, "\"hasEaten\":false").andExpect(status().isCreated());

        assertThat(result(special)).isEqualTo("BLOCKED_CONFLICT_NOT_EATEN");
        assertThat(incidentCommands.recorded()).hasSize(incidentsBefore);
    }

    @Test
    void BR_M08_14_restrictionOnly_blockedWithoutIncidentOrAlert() throws Exception {
        long beef = f.allergen();
        long dish = f.activeDish(List.of(beef), List.of(defaultDiet));
        long h = residentIn(room2);
        f.activeAssignment(h, defaultDiet, "SOFT", List.of(beef), time.now());
        publishedMenu(day.minusDays(2), dish);
        closeLunchAndServe();
        long special = specialOf(h);
        int incidentsBefore = incidentCommands.recorded().size();

        as(serverOf(floor2));
        confirm(special, h, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PORTION_VIOLATES_RESTRICTION"));
        assertThat(confirmations(special)).isZero();
        assertThat(incidentCommands.recorded()).hasSize(incidentsBefore);
        assertThat(events.stream(AlertRequested.class)).isEmpty();
    }

    @Test
    void BR_M08_14_offline_deviceTimeKeptSeparately_conflictCheckedAgainAtSync() throws Exception {
        long tom = f.allergen();
        long shrimp = f.activeDish(List.of(tom), List.of(defaultDiet));
        publishedMenu(day.minusDays(2), shrimp);
        long ok = softResident(room2);
        long late = softResident(room2);
        closeLunchAndServe();
        long forOk = specialOf(ok);
        long forLate = specialOf(late);
        allergy(late, tom);
        var deviceAt = at(LocalTime.of(11, 5));
        var accepted = new SyncOutcome(SyncResult.ACCEPTED, "");

        as(serverOf(floor2));
        assertThat(offline.recordType()).isEqualTo("M08.SERVING_CONFIRMATION");
        offline.apply(new ServingOfflineHandler.Content(forOk, ok, null, null), accepted, deviceAt);
        assertThat(result(forOk)).isEqualTo("CORRECT_PORTION");
        assertThat(jdbc.queryForObject(
                        "SELECT device_at < occurred_at FROM record_event WHERE kind = 'M08.SERVING_CONFIRMATION'"
                                + " AND entity_id = ?",
                        Boolean.class,
                        forOk))
                .isTrue();

        assertThatThrownBy(() ->
                        offline.apply(new ServingOfflineHandler.Content(forLate, late, null, null), accepted, deviceAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PORTION_HAS_ALLERGEN");
        offline.apply(
                new ServingOfflineHandler.Content(forLate, late, false, new IncidentData(null, null, null)),
                accepted,
                deviceAt);
        assertThat(result(forLate)).isEqualTo("BLOCKED_CONFLICT_NOT_EATEN");
    }
}
