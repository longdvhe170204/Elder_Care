package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class AssignmentDraftAtomicityIT extends AssignmentHttpTestBase {
    @MockitoSpyBean
    AssignmentStore store;

    @Test
    void FR_025_failureAfterSqlUpdateRollsBackTheOriginalRow() throws Exception {
        doAnswer(call -> {
                    call.callRealMethod();
                    throw new BusinessException(
                            HttpStatus.CONFLICT, "STATE_CHANGED", "Lỗi sau ghi dùng để kiểm rollback");
                })
                .when(store)
                .updateDraft(any(), any());
        var value = body();
        value.put("staffId", other.staffId());
        mvc.perform(post("/api/v1/assignments/" + assignment + "/update-draft")
                        .header("Authorization", bearer)
                        .header("If-Match", "\"0\"")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(value)))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT staff_id FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isEqualTo(worker.staffId());
        assertThat(jdbc.queryForObject("SELECT version FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isOne();
    }
}
