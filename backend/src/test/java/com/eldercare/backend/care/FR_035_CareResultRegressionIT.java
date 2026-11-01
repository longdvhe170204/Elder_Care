package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.events.AlertClosureRequested;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.care.FakeTaskSource;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;

class FR_035_CareResultRegressionIT extends CareResultTestBase {
    @Test
    void FR_028a_reasonStoresPhysicalCatalogReferenceAndAuditsThePublicChoice() throws Exception {
        var row = jdbc.queryForMap(
                "SELECT id,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.legacy.id')) AS UNSIGNED),id) public_id FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND status='ACTIVE' ORDER BY id LIMIT 1");
        long physical = ((Number) row.get("id")).longValue(), selected = ((Number) row.get("public_id")).longValue();
        notPerform("{\"reasonTypeId\":" + selected + "}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reason_type_id FROM care_task WHERE id=?", Long.class, task))
                .isEqualTo(physical);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M04.TASK' AND entity_id=? AND JSON_EXTRACT(data_after,'$.nonPerformanceReasonTypeId')=?",
                        Long.class,
                        Long.toString(task),
                        selected))
                .isEqualTo(1);
    }

    @Autowired
    @Qualifier("fakeMealTaskSource")
    FakeTaskSource meals;

    @Test
    void FR_034_wholeMlMayContainTrailingDecimalZeroes() throws Exception {
        record("{\"numericValue\":200.000}").andExpect(status().isOk());
    }

    @Test
    void FR_035_importantLateCompletionRequiresReason() throws Exception {
        jdbc.update(
                "UPDATE care_task SET status='OVERDUE',window_to=?,importance='IMPORTANT' WHERE id=?",
                local(time.now().minusSeconds(60)),
                task);
        record("{\"numericValue\":200}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        record("{\"numericValue\":200,\"reason\":\"Lý do kiểm thử\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED_LATE"));
    }

    @Test
    void FR_035_performingWithinWindowClosesOverdueAsCompleted() throws Exception {
        Instant performed = time.now().minusSeconds(120);
        jdbc.update(
                "UPDATE care_task SET status='OVERDUE',window_to=? WHERE id=?",
                local(time.now().minusSeconds(60)),
                task);
        record("{\"numericValue\":200,\"performedAt\":\"" + performed + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(events.stream(AlertClosureRequested.class)
                        .filter(e -> e.taskId() == task)
                        .toList())
                .singleElement()
                .satisfies(e -> assertThat(e.reason()).isEqualTo("PERFORMED_IN_WINDOW"));
    }

    @Test
    void FR_040a_unconfirmedMealDoesNotCloseTask() throws Exception {
        long meal = TestUsers.nextId();
        data("EATING", false);
        jdbc.update("UPDATE care_task SET basis_type=?,basis_id=? WHERE id=?", SourceType.MEAL.name(), meal, task);
        meals.served(meal, resident, false);
        record("{\"codeValue\":\"ATE_ALL\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_NOT_CONFIRMED"));
        assertThat(storedStatus()).isEqualTo("DUE");
    }
}
