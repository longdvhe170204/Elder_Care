package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** T055, T057: bean chuyển trạng thái của 005 và tính lại khi đính chính chuyến đi (FR-010, FR-020, FR-020a, BR-M04-04). */
class BR_M04_04_ResidentStatusTasksIT extends CareOverdueTestBase {
    StatusTransitionParticipant participant() {
        return context.getBean("careStatusParticipant", StatusTransitionParticipant.class);
    }

    CareTaskStore store() {
        return context.getBean(CareTaskStore.class);
    }

    void apply(StatusCommand command, ResidentStatus from, ResidentStatus to, Instant at) {
        participant()
                .applyEffects(new TransitionResult(
                        residentId, from, to, command, TestUsers.nextId(), at, json.createObjectNode()));
    }

    String rule(long id) {
        return jdbc.queryForObject("SELECT closure_rule FROM care_task WHERE id=?", String.class, id);
    }

    @Test
    void BR_M04_04_temporaryAbsenceCancelsNotYetDueTasksFromDepartureOnly() throws Exception {
        long notYetDue = create();
        long due = createAnother();
        jdbc.update("UPDATE care_task SET status='DUE' WHERE id=?", due);

        apply(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.TEMPORARILY_ABSENT,
                time.now());

        assertThat(store().get(notYetDue, false).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(rule(notYetDue)).isEqualTo("ABSENCE_CANCELLED");
        assertThat(store().get(due, false).status()).isEqualTo(TaskStatus.DUE);
    }
}
