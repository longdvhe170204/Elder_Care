package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
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
                    "DELETE FROM correction_chain WHERE record_type='M04.CARE_RESULT' AND original_record_id IN (SELECT id FROM care_task WHERE resident_id=?)",
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

    @Test
    void BR_M04_14_floorManagerUsesMethodBForTheOriginalTaskFloor() throws Exception {
        original();
        var next = manager(floor);
        duties.floorManager(floor, next.staffId());
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Đối chiếu bản gốc\"}")
                .andExpect(status().isCreated());
        assertThat(corrections.chain("M04.CARE_RESULT", task).corrections())
                .singleElement()
                .satisfies(c -> assertThat(c.creationMethod()).isEqualTo(CreationMethod.B));
    }

    @Test
    void BR_M04_14_currentShiftLeadUsesMethodB() throws Exception {
        original();
        var next = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        assignments.assign(
                next.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        duties.shiftLead(floor, next.staffId());
        user.set(next.user());
        correct("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Ca xác minh\"}")
                .andExpect(status().isCreated());
        assertThat(corrections.chain("M04.CARE_RESULT", task).corrections())
                .singleElement()
                .satisfies(c -> assertThat(c.creationMethod()).isEqualTo(CreationMethod.B));
    }

    @Test
    void BR_M04_14_wrongFloorCannotCorrectEvenWhenDutySourceNamesTheUser() throws Exception {
        original();
        var next = manager(TestUsers.nextId());
        duties.floorManager(floor, next.staffId());
        correct("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Sai phạm vi\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_14_nonResponsiblePeerCannotCreateMethodC() throws Exception {
        original();
        manager(floor);
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Không có nhiệm vụ\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_14_voidHidesTheProjectionButRetainsTheClosedTask() throws Exception {
        original();
        correct("{\"type\":\"VOID\",\"reason\":\"Ghi nhầm hoàn toàn\"}").andExpect(status().isCreated());
        assertThat(corrections
                        .current("M04.CARE_RESULT", List.of(task))
                        .get(task)
                        .voided())
                .isTrue();
        assertThat(storedStatus()).isEqualTo("COMPLETED");
        assertThat(results.find(task)).isPresent();
        assertThat(publicResults.results(
                        resident, time.now().minusSeconds(86400), time.now().plusSeconds(1)))
                .isEmpty();
        correct("{\"type\":\"VOID\",\"reason\":\"Lặp lại\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECORD_VOIDED"));
    }

    @Test
    void BR_M04_14_missingReasonAndMalformedContentAreRejected() throws Exception {
        original();
        correct("{\"type\":\"VOID\"}").andExpect(status().isUnprocessableEntity());
        correct("{\"type\":\"AMEND_CONTENT\",\"reason\":\"Thiếu giá trị\"}")
                .andExpect(status().isUnprocessableEntity());
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"performedAt\":\"2026-01-01T00:00:00Z\"},\"reason\":\"Không đổi metadata\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void BR_M04_14_unconfirmedTaskHasNoCorrectableResult() throws Exception {
        correct("{\"type\":\"VOID\",\"reason\":\"Chưa ghi\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECORD_NOT_CONFIRMED"));
    }

    @Test
    void BR_M04_14_measuredValueMustBeCorrectedIn007() throws Exception {
        task = complete("MEASURED_VALUE", Map.of("numericValue", 100), time.now(), null);
        correct("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":90},\"reason\":\"Chuyển007\"}")
                .andExpect(status().isForbidden());
        correct("{\"type\":\"VOID\",\"reason\":\"Chuyển007\"}").andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_14_realCorrectionRecalculatesTheMealRuleAndVoidSkipsTheMeal() throws Exception {
        var at = time.now().minusSeconds(3600);
        complete("EATING", Map.of("codeValue", "SOME"), at.minusSeconds(300), TestUsers.nextId());
        long good = complete("EATING", Map.of("codeValue", "ATE_ALL"), at.minusSeconds(200), TestUsers.nextId());
        complete("EATING", Map.of("codeValue", "NOT_EATEN"), at.minusSeconds(100), TestUsers.nextId());
        long latest = complete("EATING", Map.of("codeValue", "ATE_ALL"), at, TestUsers.nextId());
        corrections.create("M04.CARE_RESULT", good, CorrectionType.VOID, null, Reason.of("Ghi nhầm bữa"));
        corrections.create(
                "M04.CARE_RESULT",
                latest,
                CorrectionType.AMEND_CONTENT,
                Map.of("codeValue", "SKIPPED"),
                Reason.of("Đối chiếu ghi nhận"));
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .singleElement()
                .satisfies(e -> assertThat(e.mealIds()).hasSize(3));
    }

    @Test
    void BR_M04_14_billableCorrectionWaitsForTheApprovedQuantityContract() throws Exception {
        data("ML", true);
        task = complete("ML", Map.of("numericValue", 200), time.now(), null);
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":100},\"reason\":\"Chờ quantity010\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("CARE_SOURCE_UNAVAILABLE"));
        assertThat(events.stream(BillableCareResultCorrected.class)).isEmpty();
    }

    @Test
    void BR_M04_14_sleepVoidWaitsForThe007EventContract() throws Exception {
        task = complete("SLEEP", Map.of("numericValue", 60, "codeValue", "TEST_SLEEP"), time.now(), null);
        correct("{\"type\":\"VOID\",\"reason\":\"Chờ tín hiệu hủy007\"}").andExpect(status().isServiceUnavailable());
    }

    @Test
    void BR_M04_14_amendedSleepPublishesTheNewGroupWithACorrectionKey() throws Exception {
        String code = "SLEEP_TEST_" + resident;
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES('M04.RESULT_VALUE','M04',?,'ACTIVE',?)",
                code,
                "{\"type\":\"SLEEP_QUALITY\",\"code\":\"" + code + "\",\"ruleGroup\":\"INSOMNIA\"}");
        task = complete(
                "SLEEP",
                Map.of("numericValue", 60, "codeValue", code),
                time.now().minusSeconds(60),
                null);
        correct("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":30,\"codeValue\":\"" + code
                        + "\"},\"reason\":\"Đối chiếu giấc ngủ\"}")
                .andExpect(status().isCreated());
        assertThat(events.stream(SleepResultRecorded.class).filter(e -> e.resultId() == task))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.valueGroup()).isEqualTo("INSOMNIA");
                    assertThat(e.context().milestone()).startsWith("CORRECTION:");
                });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void BR_M04_14_typeSpecificValidationRollsBackCorrectionAndAudit() throws Exception {
        original();
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":-1},\"reason\":\"Số âm không hợp lệ\"}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(corrections.chain("M04.CARE_RESULT", task).corrections()).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M04.CARE_RESULT' AND entity_id=? AND action='CORRECTION_ADDED'",
                        Integer.class,
                        task))
                .isZero();
        assertThat(storedStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void BR_M04_14_genericEntryWithoutSafeIsolationIsRejectedBeforeWriting() throws Exception {
        original();
        mvc.perform(
                        post("/api/v1/corrections/M04.CARE_RESULT/" + task)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Không dùng snapshot cũ\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("CARE_SOURCE_UNAVAILABLE"));
        assertThat(corrections.chain("M04.CARE_RESULT", task).corrections()).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void BR_M04_14_parallelSafeCommandsSeeBothCommittedCorrections() throws Exception {
        var at = time.now().minusSeconds(3600);
        long first = complete("EATING", Map.of("codeValue", "ATE_ALL"), at.minusSeconds(200), TestUsers.nextId());
        long second = complete("EATING", Map.of("codeValue", "ATE_ALL"), at.minusSeconds(100), TestUsers.nextId());
        complete("EATING", Map.of("codeValue", "SKIPPED"), at, TestUsers.nextId());
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> {
                start.await();
                commands.correct(
                        first,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("codeValue", "SOME"),
                        Reason.of("Đối chiếu bữa một"));
                return true;
            });
            var two = pool.submit(() -> {
                start.await();
                commands.correct(
                        second,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("codeValue", "NOT_EATEN"),
                        Reason.of("Đối chiếu bữa hai"));
                return true;
            });
            start.countDown();
            assertThat(one.get(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(two.get(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M04.POOR_EATING' AND entity_type='M01.RESIDENT' AND entity_id=?",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        assertThat(corrections.chain("M04.CARE_RESULT", first).corrections()).hasSize(1);
        assertThat(corrections.chain("M04.CARE_RESULT", second).corrections()).hasSize(1);
    }

    @Test
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    void BR_M04_14_readingTheChainInsideAnotherWriteTransactionDoesNotRequireCorrectionIsolation() throws Exception {
        original();
        assertThat(corrections.chain("M04.CARE_RESULT", task).corrections()).isEmpty();
    }

    @Test
    void BR_M04_14_genericChainCannotExposeValuesOutsideTheViewScope() throws Exception {
        original();
        manager(TestUsers.nextId());
        mvc.perform(get("/api/v1/corrections/M04.CARE_RESULT/" + task)).andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_14_genericChainCannotExposeRawValuesWithPresenceOnlyHealthVisibility() throws Exception {
        original();
        doReturn(com.eldercare.backend.administration.account.api.dto.Visibility.PRESENCE_ONLY)
                .when(visibility)
                .canView(any(), eq(com.eldercare.backend.administration.account.api.dto.InfoGroup.HEALTH), any());
        mvc.perform(get("/api/v1/corrections/M04.CARE_RESULT/" + task)).andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_14_existingDetailAndHistoryExposeCurrentValuesAndCancellationFlags() throws Exception {
        original();
        correct(
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":200},\"reason\":\"Đối chiếu lượng nước\"}")
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currentValue.numericValue").value(200))
                .andExpect(jsonPath("$.result.corrected").value(true));
        mvc.perform(get("/api/v1/residents/" + resident + "/care-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentValue.numericValue").value(200))
                .andExpect(jsonPath("$[0].corrected").value(true));
        correct("{\"type\":\"VOID\",\"reason\":\"Ghi nhầm hoàn toàn\"}").andExpect(status().isCreated());
        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(jsonPath("$.result.recordingCancelled").value(true));
        mvc.perform(get("/api/v1/residents/" + resident + "/care-results"))
                .andExpect(jsonPath("$[0].recordingCancelled").value(true));
        assertThat(storedStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void BR_M04_14_cleanupPreservesOtherEntityTypesWithTheSameNumericId() throws Exception {
        String planEvent = UUID.randomUUID().toString();
        String otherCorrection = UUID.randomUUID().toString();
        long otherResident = TestUsers.nextId();
        var at = java.sql.Timestamp.from(time.now());
        String insert =
                "INSERT INTO EVENT_PUBLICATION(ID,LISTENER_ID,EVENT_TYPE,SERIALIZED_EVENT,PUBLICATION_DATE,COMPLETION_DATE,STATUS) VALUES(?,?,?,?,?,?,'COMPLETED')";
        try {
            jdbc.update(
                    insert,
                    planEvent,
                    "fixture-preservation",
                    "com.eldercare.backend.care.api.events.CarePlanReturned",
                    "{\"residentId\":" + otherResident + ",\"context\":{\"sourceType\":\"M04.CARE_PLAN\",\"sourceId\":"
                            + task + "}}",
                    at,
                    at);
            jdbc.update(
                    insert,
                    otherCorrection,
                    "fixture-preservation",
                    "com.eldercare.backend.administration.correction.api.events.CorrectionMade",
                    "{\"recordType\":\"M01.ASSESSMENT\",\"originalRecordId\":" + task + "}",
                    at,
                    at);
            cleanCommittedFixture();
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE ID IN (?,?)",
                            Integer.class,
                            planEvent,
                            otherCorrection))
                    .isEqualTo(2);
        } finally {
            jdbc.update("DELETE FROM EVENT_PUBLICATION WHERE ID IN (?,?)", planEvent, otherCorrection);
        }
    }
}
