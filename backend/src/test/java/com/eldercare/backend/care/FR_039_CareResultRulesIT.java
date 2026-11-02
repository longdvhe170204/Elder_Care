package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.administration.correction.api.events.CorrectionMade;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.careplan.service.CarePlanQueries;
import com.eldercare.backend.care.internal.result.domain.CareResultInput;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** T048: MySQL thật; spy chỉ cung cấp projection đính chính/plan khi provider nguồn chưa đủ. */
class FR_039_CareResultRulesIT extends CareResultTestBase {
    @Autowired
    ApplicationContext context;

    @Autowired
    CareResultStore resultStore;

    @Autowired
    CareTaskStore taskStore;

    @MockitoSpyBean
    CarePlanQueries plans;

    @MockitoSpyBean
    CorrectionApi corrections;

    @MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentStatusApi statuses;

    @org.junit.jupiter.api.BeforeEach
    void presence() {
        doReturn(List.of(new com.eldercare.backend.resident.api.dto.StatusPeriod(
                        com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE,
                        time.today()
                                .atStartOfDay(com.eldercare.backend.common.time.BusinessTime.ZONE)
                                .toInstant(),
                        Optional.empty())))
                .when(statuses)
                .statusPeriods(eq(resident), anySet(), any(), any());
    }

    Object rules() {
        return context.getBean("careResultRules");
    }

    void recalculate(long id) throws Exception {
        rules().getClass().getMethod("onResult", long.class).invoke(rules(), id);
    }

    void water(Instant at) throws Exception {
        rules().getClass()
                .getMethod("water", long.class, LocalDate.class, Instant.class)
                .invoke(rules(), resident, time.today(), at);
    }

    long save(String kind, String code, Integer ml, Instant at, Long meal) {
        return save(kind, code, ml, at, meal, at);
    }

    long save(String kind, String code, Integer ml, Instant at, Long meal, Instant planned) {
        // Fixture có thể mô phỏng mốc cuối ngày khi test thực chạy trước mốc; không ghi trước lúc thực hiện.
        var recordedAt = time.now();
        if (at.isAfter(recordedAt)) recordedAt = at;
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at) SELECT ?,resident_id,task_type_id,?,?,shift_id,floor_area_id,?,?,? ,importance,performer_role,performer_id,'DUE',JSON_SET(data,'$.resultType',?),created_by,created_at FROM care_task WHERE id=?",
                id,
                meal == null ? "AD_HOC" : "MEAL",
                meal,
                local(planned),
                local(at.minusSeconds(900)),
                local(at.plusSeconds(900)),
                kind,
                task);
        var input =
                new CareResultInput(at, ml == null ? null : BigDecimal.valueOf(ml), code, Map.of(), false, null, null);
        resultStore.complete(
                taskStore.get(id, false),
                input,
                TaskStatus.COMPLETED,
                at,
                recordedAt,
                null,
                user.accountId(),
                "ASSIGNED",
                false,
                false,
                null);
        return id;
    }

    @Test
    void FR_040_latestPlannedMealControlsStreakAndCorrectionUsesCurrentValue() throws Exception {
        var at = time.now().minusSeconds(3600);
        save("EATING", "SOME", null, at.minusSeconds(300), TestUsers.nextId());
        save("EATING", "NOT_EATEN", null, at.minusSeconds(200), TestUsers.nextId());
        long latest = save("EATING", "ATE_ALL", null, at, TestUsers.nextId());
        doReturn(Map.of(latest, new CurrentValue(Map.of("codeValue", "SKIPPED"), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        recalculate(latest);
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .singleElement()
                .satisfies(e -> assertThat(e.mealIds()).hasSize(3));
        recalculate(latest);
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
    }

    @Test
    void FR_040_voidedGoodMealIsSkippedRatherThanBreakingTheStreak() throws Exception {
        var at = time.now().minusSeconds(3600);
        save("EATING", "SOME", null, at.minusSeconds(400), TestUsers.nextId());
        long voided = save("EATING", "ATE_ALL", null, at.minusSeconds(300), TestUsers.nextId());
        save("EATING", "NOT_EATEN", null, at.minusSeconds(200), TestUsers.nextId());
        long latest = save("EATING", "SKIPPED", null, at, TestUsers.nextId());
        doReturn(Map.of(voided, new CurrentValue(null, true, true)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        recalculate(latest);
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
    }

    @Test
    void FR_039_waterReadsCorrectedMlAndChecksOnlyOncePerDayWithoutCreatingTasks() throws Exception {
        var at = time.today()
                .atTime(16, 0)
                .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        long id = save("ML", null, 200, at.minusSeconds(3600), null);
        var item = new CarePlanItemView(
                TestUsers.nextId(),
                type,
                "Nước",
                "TIMES_PER_DAY",
                Optional.empty(),
                Optional.of(1),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                15,
                15,
                com.eldercare.backend.common.Role.CAREGIVER,
                ResultType.ML,
                Importance.NORMAL,
                false,
                Optional.of(1500));
        doReturn(Optional.of(new CarePlanView(
                        TestUsers.nextId(),
                        resident,
                        1,
                        CarePlanStatus.EFFECTIVE,
                        time.today(),
                        Optional.empty(),
                        List.of(item))))
                .when(plans)
                .effectiveOn(resident, time.today());
        doReturn(Map.of(id, new CurrentValue(Map.of("numericValue", 1000), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        int count = jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE resident_id=?", Integer.class, resident);
        water(at);
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .isEmpty();
        doReturn(Map.of()).when(corrections).current(eq("M04.CARE_RESULT"), anyCollection());
        water(at.plusSeconds(60));
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(count);
    }

    @Test
    void FR_042_voidRemovesADayAndBreaksMoodStreak() throws Exception {
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES('M04.RESULT_VALUE','M04',?,'ACTIVE',?)",
                "Mood " + TestUsers.nextId(),
                "{\"type\":\"MOOD\",\"code\":\"NEG_TEST_" + resident + "\",\"ruleGroup\":\"NEGATIVE\"}");
        var at = time.now();
        String code = "NEG_TEST_" + resident;
        save("MOOD", code, null, at.minusSeconds(172800), null);
        long middle = save("MOOD", code, null, at.minusSeconds(86400), null);
        long latest = save("MOOD", code, null, at, null);
        doReturn(Map.of(middle, new CurrentValue(null, true, true)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        recalculate(latest);
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .isEmpty();
    }

    @Test
    void FR_039_lowWaterEmitsOneLightAlertAndNoTasks() throws Exception {
        var at = time.today()
                .atTime(16, 0)
                .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        save("ML", null, 200, at.minusSeconds(3600), null);
        var item = new CarePlanItemView(
                TestUsers.nextId(),
                type,
                "Nước",
                "TIMES_PER_DAY",
                Optional.empty(),
                Optional.of(1),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                15,
                15,
                com.eldercare.backend.common.Role.CAREGIVER,
                ResultType.ML,
                Importance.NORMAL,
                false,
                Optional.of(1500));
        doReturn(Optional.of(new CarePlanView(
                        TestUsers.nextId(),
                        resident,
                        1,
                        CarePlanStatus.EFFECTIVE,
                        time.today(),
                        Optional.empty(),
                        List.of(item))))
                .when(plans)
                .effectiveOn(resident, time.today());
        water(at);
        water(at.plusSeconds(60));
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .singleElement()
                .satisfies(e -> assertThat(e.level())
                        .isEqualTo(com.eldercare.backend.common.notification.NotificationLevel.LOW));
    }

    @Test
    void FR_040_recordingCommandInvokesTheRuleOnTheThirdMeal() throws Exception {
        var at = time.now().minusSeconds(600);
        save("EATING", "SOME", null, at.minusSeconds(200), TestUsers.nextId());
        save("EATING", "NOT_EATEN", null, at.minusSeconds(100), TestUsers.nextId());
        long meal = TestUsers.nextId();
        data("EATING", false);
        jdbc.update("UPDATE care_task SET basis_type='MEAL',basis_id=? WHERE id=?", meal, task);
        record("{\"codeValue\":\"SKIPPED\"}").andExpect(status().isOk());
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
    }

    @Test
    void FR_040_correctionEventRecalculatesAndLateOlderMealCannotReplaceLatestGoodMeal() throws Exception {
        var at = time.now().minusSeconds(600);
        save("EATING", "SOME", null, at.minusSeconds(400), TestUsers.nextId());
        save("EATING", "NOT_EATEN", null, at.minusSeconds(300), TestUsers.nextId());
        long good = save("EATING", "ATE_ALL", null, at, TestUsers.nextId());
        long late = save("EATING", "SKIPPED", null, at.plusSeconds(100), TestUsers.nextId(), at.minusSeconds(200));
        recalculate(late);
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .isEmpty();
        doReturn(Map.of(good, new CurrentValue(Map.of("codeValue", "SKIPPED"), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        context.publishEvent(
                new CorrectionMade("M04.CARE_RESULT", good, CorrectionType.AMEND_CONTENT, TestUsers.nextId()));
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .singleElement()
                .satisfies(e -> assertThat(e.mealIds()).hasSize(4));
    }

    @Test
    void FR_042_threeNegativeCalendarDaysEmitOneAlertForAssignedNurse() throws Exception {
        String code = "NEG_TEST_" + resident;
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES('M04.RESULT_VALUE','M04',?,'ACTIVE',?)",
                "Mood " + TestUsers.nextId(),
                "{\"type\":\"MOOD\",\"code\":\"" + code + "\",\"ruleGroup\":\"NEGATIVE\"}");
        var at = time.now();
        save("MOOD", code, null, at.minusSeconds(172800), null);
        save("MOOD", code, null, at.minusSeconds(86400), null);
        long latest = save("MOOD", code, null, at, null);
        recalculate(latest);
        recalculate(latest);
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.type()).isEqualTo("PROLONGED_NEGATIVE_MOOD");
                    assertThat(e.context().recipients())
                            .singleElement()
                            .isInstanceOf(com.eldercare.backend.common.notification.RecipientGroup.AssignedNurse.class);
                });
    }

    @Test
    void FR_040_oldUnlinkedGoodResultCannotBlockThreeNewLinkedMeals() throws Exception {
        var at = time.now().minusSeconds(600);
        save("EATING", "ATE_ALL", null, at.minusSeconds(400), null);
        save("EATING", "SOME", null, at.minusSeconds(300), TestUsers.nextId());
        save("EATING", "NOT_EATEN", null, at.minusSeconds(200), TestUsers.nextId());
        long latest = save("EATING", "SKIPPED", null, at, TestUsers.nextId());
        recalculate(latest);
        assertThat(events.stream(ProlongedPoorEating.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
    }

    @Test
    void FR_040_correctedOlderMealChangesTheDownstreamEventKey() throws Exception {
        var at = time.now().minusSeconds(600);
        long good = save("EATING", "ATE_ALL", null, at.minusSeconds(400), TestUsers.nextId());
        save("EATING", "SOME", null, at.minusSeconds(300), TestUsers.nextId());
        save("EATING", "NOT_EATEN", null, at.minusSeconds(200), TestUsers.nextId());
        long latest = save("EATING", "SKIPPED", null, at, TestUsers.nextId());
        recalculate(latest);
        doReturn(Map.of(good, new CurrentValue(Map.of("codeValue", "SOME"), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        recalculate(good);
        assertThat(events.stream(ProlongedPoorEating.class)
                        .filter(e -> e.residentId() == resident)
                        .map(CareEvent::eventKey)
                        .distinct())
                .hasSize(2);
    }

    @Test
    void FR_040_unknownCorrectedEatingCodeFailsBeforeMarkingTheChain() throws Exception {
        var at = time.now().minusSeconds(600);
        long id = save("EATING", "SOME", null, at, TestUsers.nextId());
        doReturn(Map.of(id, new CurrentValue(Map.of("codeValue", "UNSUPPORTED"), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        assertThatThrownBy(() -> recalculate(id))
                .hasRootCauseInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
    }

    void waterPlan() {
        var item = new CarePlanItemView(
                TestUsers.nextId(),
                type,
                "Nước",
                "TIMES_PER_DAY",
                Optional.empty(),
                Optional.of(1),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                15,
                15,
                com.eldercare.backend.common.Role.CAREGIVER,
                ResultType.ML,
                Importance.NORMAL,
                false,
                Optional.of(1500));
        doReturn(Optional.of(new CarePlanView(
                        TestUsers.nextId(),
                        resident,
                        1,
                        CarePlanStatus.EFFECTIVE,
                        time.today(),
                        Optional.empty(),
                        List.of(item))))
                .when(plans)
                .effectiveOn(resident, time.today());
    }

    @Test
    void FR_039_invalidCorrectedMlDoesNotConsumeDailyMarker() throws Exception {
        waterPlan();
        var at = time.today()
                .atTime(16, 0)
                .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        long id = save("ML", null, 200, at.minusSeconds(3600), null);
        doReturn(Map.of(id, new CurrentValue(Map.of("numericValue", -1), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        assertThatThrownBy(() -> water(at))
                .hasRootCauseInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        doReturn(Map.of(id, new CurrentValue(Map.of("numericValue", new BigDecimal("1.5")), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        assertThatThrownBy(() -> water(at))
                .hasRootCauseInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        doReturn(Map.of()).when(corrections).current(eq("M04.CARE_RESULT"), anyCollection());
        water(at);
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
    }

    @Test
    void FR_039_presenceMinutesProrateTheTargetAndBeforeMilestoneDoesNotConsumeMarker() throws Exception {
        waterPlan();
        var at = time.today()
                .atTime(16, 0)
                .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        doReturn(List.of(new com.eldercare.backend.resident.api.dto.StatusPeriod(
                        com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE,
                        at.minusSeconds(8 * 3600),
                        Optional.empty())))
                .when(statuses)
                .statusPeriods(eq(resident), anySet(), any(), any());
        save("ML", null, 500, at.minusSeconds(3600), null);
        water(at.minusSeconds(1));
        assertThat(context.getBean(com.eldercare.backend.common.data.ReminderMarkers.class)
                        .isMarked(
                                "M04.WATER_CHECK",
                                "M01.RESIDENT",
                                resident,
                                time.today().toString()))
                .isFalse();
        water(at);
        assertThat(events.stream(AlertRequested.class).filter(e -> e.residentId() == resident))
                .isEmpty();
    }

    @Test
    void FR_042_unknownCorrectedMoodCodeCannotSilentlyBreakTheStreak() throws Exception {
        long id = save("MOOD", "UNKNOWN", null, time.now(), null);
        doReturn(Map.of(id, new CurrentValue(Map.of("codeValue", "MISSING_CODE"), true, false)))
                .when(corrections)
                .current(eq("M04.CARE_RESULT"), anyCollection());
        assertThatThrownBy(() -> recalculate(id))
                .hasRootCauseInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
    }
}
