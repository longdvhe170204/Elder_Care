package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Đề xuất 2026-11-23 lượt 3 (góp ý test demo BF-06): thời điểm chuyển viện không gửi thì là lúc thực hiện lệnh, gửi thì
 * không trước lúc xảy ra sự cố và không sau hiện tại; người cao tuổi được Ghi nhận trở về thì sự cố còn mở đã chuyển viện
 * ghi diễn biến hệ thống "Đã về viện" đúng một lần.
 */
class BR_45b_TransferTimeAndReturnIT extends EmergencyTestBase {
    static final String TRANSFER = "/api/v1/incidents/{id}/transfer-out";

    @Autowired
    IncidentProgressionStore progressions;

    @Autowired
    ApplicationEventPublisher events;

    @Test
    void transferWithoutTimeUsesNow() throws Exception {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        accepted();

        post(doctor, TRANSFER, id, body(null)).andExpect(status().isCreated());

        var transferred = progressions.list(id).stream()
                .filter(p -> "TRANSFERRED_OUT".equals(p.get("typeCode")))
                .findFirst()
                .orElseThrow();
        assertThat((Instant) transferred.get("occurredAt"))
                .isBetween(time.now().minus(Duration.ofMinutes(1)), time.now().plus(Duration.ofMinutes(1)));
    }

    @Test
    void transferBeforeIncidentOrInFutureIsRefused() throws Exception {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        accepted();

        post(doctor, TRANSFER, id, body(time.now().minus(Duration.ofDays(1))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Thời điểm chuyển không được trước lúc xảy ra sự cố"));
        post(doctor, TRANSFER, id, body(time.now().plus(Duration.ofHours(1))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Thời điểm chuyển không được sau thời điểm hiện tại"));
        assertThat(count("M05.TRANSFER_SUMMARY", id)).isZero();
    }

    @Test
    void returnFromHospitalAddsProgressionOnce() throws Exception {
        long transferredId = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        long otherId = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        accepted();
        post(doctor, TRANSFER, transferredId, body(null)).andExpect(status().isCreated());

        var returned = new ResidentStatusChanged(
                resident,
                ResidentStatus.HOSPITALIZED,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.RECORD_RETURN,
                2L,
                time.now());
        events.publishEvent(returned);
        events.publishEvent(returned);

        assertThat(returnedCount(transferredId)).isEqualTo(1);
        assertThat(returnedCount(otherId)).isZero();
    }

    long returnedCount(long incidentId) {
        return progressions.list(incidentId).stream()
                .filter(p -> "RETURNED_FROM_HOSPITAL".equals(p.get("typeCode")))
                .count();
    }

    void accepted() {
        doAnswer(call -> new TransitionResult(
                        resident,
                        ResidentStatus.IN_RESIDENCE,
                        ResidentStatus.HOSPITALIZED,
                        StatusCommand.TRANSFER_OUT,
                        1L,
                        time.now(),
                        call.getArgument(4)))
                .when(statuses)
                .execute(eq(resident), eq(StatusCommand.TRANSFER_OUT), any(), any(), any(), any());
    }

    String body(Instant at) {
        return "{\"transferUnit\":\"Bệnh viện X\"," + (at == null ? "" : "\"transferAt\":\"" + at + "\",")
                + "\"companion\":\"Con gái M\",\"handoverStaffId\":" + nurse.staffId() + "}";
    }
}
