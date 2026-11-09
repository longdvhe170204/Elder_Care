package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.events.EndingExceptionApproved;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T057, T106 (US7; FR-064, Q-379, Q-313; loại yêu cầu {@code M02.ENDING_EXCEPTION}): Hành chính lập ngoại lệ cho điều
 * kiện (a) → (d), (h); Kế toán chỉ với ý tiền của (h); Quản lý viện duyệt; ngoại lệ miễn đúng mã, không bao giờ miễn
 * bàn giao (e) hay ý "Kế toán đã xác nhận quyết toán".
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingExceptionIT extends EndingTestBase {

    @Test
    void FR_064_anApprovedException_marksTheLineMetByException_andLetsTheCommandPass() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        otherUnmet(D_ALERTS, true);
        assertThat(unmetCodes(endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0))))
                .containsExactly(D_ALERTS);

        long requestId = approvedException(ready.endingId(), D_ALERTS);

        JsonNode list = conditions(ready.endingId());
        assertThat(line(list, D_ALERTS).get("status").asString()).isEqualTo("MET_BY_EXCEPTION");
        assertThat(line(list, D_ALERTS)
                        .get("exception")
                        .get("approvalRequestId")
                        .asLong())
                .isEqualTo(requestId);
        assertThat(jdbc.queryForMap(
                                "SELECT content ->> '$.conditionCode' AS condition_code, content ->> '$.endingId' AS ending_id,"
                                        + " entity_id AS resident_id FROM record_event WHERE kind = 'M02.ENDING_CONDITION_EXCEPTION' AND content ->> '$.approvalRequestId' = ?",
                                requestId)
                        .get("condition_code"))
                .isEqualTo(D_ALERTS);
        assertThat(events.stream(EndingExceptionApproved.class)
                        .filter(e -> e.conditionCode().equals(D_ALERTS)))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.residencyEndingId()).isEqualTo(ready.endingId());
                    assertThat(e.residentId()).isEqualTo(ready.residentId());
                });

        endResidency(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));

        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
        assertThat(jdbc.queryForObject(
                        "SELECT condition_waiver FROM " + com.eldercare.backend.support.ResidentHistoryRows.STATUS
                                + " WHERE resident_id = ?" + " AND command = 'END_RESIDENCY'",
                        String.class,
                        ready.residentId()))
                .contains(D_ALERTS)
                .contains(String.valueOf(requestId));
    }
}
