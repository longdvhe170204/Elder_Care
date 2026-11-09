package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.correction.api.*;
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
        var present = List.of(new com.eldercare.backend.resident.api.dto.StatusPeriod(
                com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE,
                time.today()
                        .atStartOfDay(com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .toInstant(),
                Optional.empty()));
        doAnswer(invocation -> ((Set<?>) invocation.getArgument(1))
                                .contains(com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE)
                        ? present
                        : List.of())
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
}
