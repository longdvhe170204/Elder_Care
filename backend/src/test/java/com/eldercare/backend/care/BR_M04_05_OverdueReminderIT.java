package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskDueStep;
import java.time.*;
import org.junit.jupiter.api.Test;

/** T042: mốc đọc lại từ MySQL, không giả định schema test rỗng. */
class BR_M04_05_OverdueReminderIT extends CareOverdueTestBase {
    @Test
    void BR_M04_06_importantTaskNotifiesOnlyFloorManager() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET importance='IMPORTANT' WHERE id=?", id);
        var task = context.getBean(CareTaskStore.class).get(id, false);
        context.getBean(CareTaskDueStep.class).run(task.windowTo());
        assertThat(recorded.stream(OverdueTaskNeedsHandling.class).filter(e -> e.taskId() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.context().recipients())
                        .containsExactly(new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                                floor, new com.eldercare.backend.common.notification.Substitution.Default())));
    }

    @Test
    void BR_M04_06_mandatoryLocationTaskIncludesTargetAndSourceMetadata() throws Exception {
        long id = create();
        long location = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update(
                "UPDATE care_task SET resident_id=NULL,location_type='ROOM',location_id=?,importance='MANDATORY' WHERE id=?",
                location,
                id);
        var task = context.getBean(CareTaskStore.class).get(id, false);
        context.getBean(CareTaskDueStep.class).run(task.windowTo());
        assertThat(recorded.stream(AlertRequested.class).filter(e -> e.context().sourceId() == id))
                .singleElement()
                .satisfies(e -> {
                    var payload = json.readTree(json.writeValueAsString(e));
                    assertThat(payload.path("residentId").isNull()).isTrue();
                    assertThat(payload.path("source").asString()).isEqualTo("M04.TASK:" + id);
                    assertThat(payload.path("basisType").asString()).isEqualTo("MEAL");
                    assertThat(payload.path("basisId").asLong()).isEqualTo(sourceId);
                    var details = payload.path("sourceDetails");
                    assertThat(details.path("locationType").asString()).isEqualTo("ROOM");
                    assertThat(details.path("locationId").asLong()).isEqualTo(location);
                    assertThat(details.path("floorAreaId").asLong()).isEqualTo(floor);
                    assertThat(details.path("taskTypeId").asLong()).isEqualTo(typeId);
                    assertThat(details.path("taskTypeName").asString())
                            .isEqualTo(task.data().taskTypeName());
                    assertThat(details.path("plannedAt").asString())
                            .isEqualTo(task.plannedAt().toString());
                    assertThat(details.path("windowFrom").asString())
                            .isEqualTo(task.windowFrom().toString());
                    assertThat(details.path("windowTo").asString())
                            .isEqualTo(task.windowTo().toString());
                    assertThat(details.path("importance").asString()).isEqualTo("MANDATORY");
                });
    }

    @Test
    void BR_M04_05_dueTaskBecomesOverdueAtWindowEndAndRemindsOnlyOnce() throws Exception {
        long id = create();
        var store = context.getBean(CareTaskStore.class);
        var step = context.getBean(CareTaskDueStep.class);
        var task = store.get(id, false);
        step.run(task.windowFrom());
        assertThat(store.get(id, false).status()).isEqualTo(TaskStatus.DUE);
        step.run(task.windowTo().minusNanos(1000));
        assertThat(store.get(id, false).status()).isEqualTo(TaskStatus.DUE);
        step.run(task.windowTo());
        assertThat(store.get(id, false).status()).isEqualTo(TaskStatus.OVERDUE);
        step.run(task.windowTo().plusSeconds(60));
        assertThat(recorded.stream(OverdueTaskReminder.class).filter(e -> e.taskId() == id))
                .hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isEqualTo(1);
    }

    @Test
    void BR_M04_06_importantAndMandatoryEscalateOnce() throws Exception {
        for (String level : new String[] {"IMPORTANT", "MANDATORY"}) {
            long id = create();
            jdbc.update("UPDATE care_task SET importance=? WHERE id=?", level, id);
            var task = context.getBean(CareTaskStore.class).get(id, false);
            var step = context.getBean(CareTaskDueStep.class);
            step.run(task.windowFrom());
            step.run(task.windowTo());
            step.run(task.windowTo().plusSeconds(60));
            if (level.equals("IMPORTANT"))
                assertThat(recorded.stream(OverdueTaskNeedsHandling.class).filter(e -> e.taskId() == id))
                        .hasSize(1);
            else
                assertThat(recorded.stream(AlertRequested.class)
                                .filter(e -> e.context().sourceId() == id))
                        .hasSize(1);
            // Căn cứ mới cho vòng kế tiếp, không lập trùng.
            sourceId++;
            meals.validation(
                    com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType.MEAL,
                    sourceId,
                    residentId,
                    planned,
                    new com.eldercare.backend.care.api.spi.TaskSourcePort.SourceValidation(true, true, true, false));
        }
    }

    @Test
    void BR_M04_06_normalEscalatesFromActualOverdueTimeAfterConfiguredDelay() throws Exception {
        long id = create();
        var task = context.getBean(CareTaskStore.class).get(id, false);
        var step = context.getBean(CareTaskDueStep.class);
        var at = task.windowTo().plusSeconds(600);
        step.run(at);
        var delay = context.getBean(com.eldercare.backend.administration.parameter.api.ParameterApi.class)
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M04_12)
                .asDuration();
        step.run(at.plus(delay).minusNanos(1000));
        assertThat(recorded.stream(OverdueTaskNeedsHandling.class).filter(e -> e.taskId() == id))
                .isEmpty();
        step.run(at.plus(delay));
        step.run(at.plus(delay).plusSeconds(60));
        assertThat(recorded.stream(OverdueTaskNeedsHandling.class).filter(e -> e.taskId() == id))
                .hasSize(1);
    }
}
