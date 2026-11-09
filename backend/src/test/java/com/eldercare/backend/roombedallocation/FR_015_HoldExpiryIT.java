package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.support.JobLogCapture;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

class FR_015_HoldExpiryIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @MockitoSpyBean
    FakeAllocationResidencySource source;

    private void run() {
        ReflectionTestUtils.invokeMethod(context.getBean("holdExpiryJob"), "runNow");
    }

    private long expired(String reason) {
        long resident = TestUsers.nextId();
        long waiting = TestUsers.nextId();
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason=?,hold_resident_id=?,hold_waiting_record_id=?,hold_until='2000-01-01' WHERE id=?",
                reason,
                resident,
                reason.equals("WAITING_RECORD") ? waiting : null,
                bed);
        return waiting;
    }

    @Test
    void FR_015_waitingHoldExpiresOnceAndCalls004InTheSameTransaction() {
        long waiting = expired("WAITING_RECORD");
        try (var log = new JobLogCapture()) {
            run();
            String runId = jdbc.queryForObject(
                    "SELECT JSON_UNQUOTE(JSON_EXTRACT(basis,'$.schedulerRunId')) FROM audit_log"
                            + " WHERE action='M03_BED_STATE_CHANGED' AND entity_type='M03.BED' AND entity_id=?"
                            + " ORDER BY id DESC LIMIT 1",
                    String.class,
                    Long.toString(bed));
            assertThat(runId).isNotNull();
            assertThat(UUID.fromString(runId).toString()).isEqualTo(runId);
            assertThat(log.events(runId)).hasSize(2);
            assertThat(log.events(runId).getFirst())
                    .containsEntry("job", "EXPIRE_BED_HOLDS")
                    .containsEntry("event", "START");
            assertThat(log.result(runId)).isEqualTo("SUCCEEDED");
        }
        run();
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
        verify(source, times(1)).waitingRecordHoldExpired(waiting);
        assertThat(jdbc.queryForObject(
                        "SELECT executed_late FROM bed_state_history WHERE bed_id=?", Boolean.class, bed))
                .isTrue();
    }
}
