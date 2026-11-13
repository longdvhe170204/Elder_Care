package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.result.domain.CareResultInput;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.result.service.PublicCareResults;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.*;

/** T050: CorrectionApi, quyền và MySQL thật; chỉ nguồn liên module dùng fake chung. */
@Transactional(isolation = Isolation.READ_COMMITTED)
class BR_M04_14_CareResultCorrectionIT extends CareResultTestBase {
    @org.junit.jupiter.api.AfterEach
    void cleanCommittedFixture() throws Exception {
        if (org.springframework.test.context.transaction.TestTransaction.isActive() || resident == 0) return;
        // Chỉ dọn các dòng của resident fixture này; test không có outer transaction phải tự hoàn tác.
        jdbc.update(
                "DELETE FROM EVENT_PUBLICATION WHERE (EVENT_TYPE LIKE 'com.eldercare.backend.care.%' AND (JSON_EXTRACT(SERIALIZED_EVENT,'$.residentId')=? OR (JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.context.sourceType')) IN ('M04.TASK','M04.CARE_RESULT') AND JSON_EXTRACT(SERIALIZED_EVENT,'$.context.sourceId') IN (SELECT id FROM care_task WHERE resident_id=?)))) OR (EVENT_TYPE='com.eldercare.backend.administration.correction.api.events.CorrectionMade' AND JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.recordType'))='M04.CARE_RESULT' AND JSON_EXTRACT(SERIALIZED_EVENT,'$.originalRecordId') IN (SELECT id FROM care_task WHERE resident_id=?))",
                resident, resident, resident);
        try (var connection = retentionConnection()) {
            for (String sql : List.of(
                    "DELETE FROM correction WHERE record_type='M04.CARE_RESULT' AND original_record_id IN (SELECT id FROM care_task WHERE resident_id=?)",
                    "DELETE FROM audit_log WHERE entity_type IN ('M04.CARE_RESULT','M04.TASK') AND entity_id IN (SELECT id FROM care_task WHERE resident_id=?)",
                    "DELETE FROM status_history WHERE entity_type='M04.TASK' AND entity_id IN (SELECT id FROM care_task WHERE resident_id=?)",
                    "DELETE FROM reminder_marker WHERE kind LIKE 'M04.%' AND entity_type='M01.RESIDENT' AND entity_id=?",
                    "DELETE FROM care_task WHERE resident_id=?",
                    "DELETE FROM resident_module_state WHERE resident_id=?")) {
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, resident);
                    statement.executeUpdate();
                }
            }
        }
    }

    @Autowired
    CorrectionApi corrections;

    @Autowired
    CareResultStore results;

    @Autowired
    CareTaskStore tasks;

    @Autowired
    PublicCareResults publicResults;

    @Autowired
    FakeDuties duties;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.account.api.FieldVisibilityApi visibility;

    @Autowired
    com.eldercare.backend.care.internal.result.service.CareResultCorrectionService commands;

    ResultActions correct(String body) throws Exception {
        return mvc.perform(post("/api/v1/care-results/" + task + "/correct")
                .header("Idempotency-Key", "correct-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    void original() throws Exception {
        record("{\"numericValue\":2000,\"upperLimitConfirmed\":true}").andExpect(status().isOk());
    }

    long complete(String kind, Map<String, Object> values, Instant at, Long meal) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at) SELECT ?,resident_id,task_type_id,?,?,shift_id,floor_area_id,?,?,?,importance,performer_role,performer_id,'DUE',JSON_SET(data,'$.resultType',?),created_by,created_at FROM care_task WHERE id=?",
                id,
                meal == null ? "AD_HOC" : "MEAL",
                meal,
                local(at),
                local(at.minusSeconds(900)),
                local(at.plusSeconds(900)),
                kind,
                task);
        var numeric = values.get("numericValue");
        @SuppressWarnings("unchecked")
        var json = (Map<String, Object>) values.getOrDefault("jsonValue", Map.of());
        results.complete(
                tasks.get(id, false),
                new CareResultInput(
                        at,
                        numeric == null ? null : new BigDecimal(numeric.toString()),
                        (String) values.get("codeValue"),
                        json,
                        true,
                        null,
                        null),
                TaskStatus.COMPLETED,
                at,
                time.now(),
                null,
                user.accountId(),
                "ASSIGNED",
                false,
                false,
                null);
        return id;
    }

    TestUsers.TestUser manager(long assignedFloor) {
        var next = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                next.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        assignedFloor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(3600),
                        null));
        user.set(next.user());
        return next;
    }

    @Test
    void BR_M04_14_originalRecorderAmendsAndOriginalTaskRemainsImmutable() throws Exception {
        original();
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Ghi nhầm lượng nước\"}")
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
        var chain = corrections.chain("M04.CARE_RESULT", task);
        assertThat(chain.corrections())
                .singleElement()
                .satisfies(c -> assertThat(c.creationMethod()).isEqualTo(CreationMethod.A));
        assertThat(new BigDecimal(((Map<?, ?>) chain.originalContent())
                        .get("numericValue")
                        .toString()))
                .isEqualByComparingTo("2000");
        assertThat(new BigDecimal(
                        ((Map<?, ?>) chain.currentContent()).get("numericValue").toString()))
                .isEqualByComparingTo("200");
        assertThat(results.find(task).orElseThrow().values().get("numericValue").toString())
                .isEqualTo("2000");
        assertThat(storedStatus()).isEqualTo("COMPLETED");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_task SET result_values=JSON_OBJECT('numericValue',1) WHERE id=?", task))
                .hasMessageContaining("TASK_CLOSED");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM care_task WHERE id=?", task))
                .rootCause()
                .hasMessageContaining("DELETE command denied");
        assertThat(publicResults.results(
                        resident, time.now().minusSeconds(86400), time.now().plusSeconds(1)))
                .singleElement()
                .satisfies(v -> assertThat(
                                new BigDecimal(v.values().get("numericValue").toString()))
                        .isEqualByComparingTo("200"));
    }
}
