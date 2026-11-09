package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.api.events.AlertRequested;
import com.eldercare.backend.nutrition.internal.serving.service.ServingOfflineHandler;
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
}
