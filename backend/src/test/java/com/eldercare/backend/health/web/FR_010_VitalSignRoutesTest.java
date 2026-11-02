package com.eldercare.backend.health.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.health.api.dto.*;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** FR-010/016: định tuyến HTTP tiếng Anh cho lịch sử, ghi và đo lại chỉ số. */
class FR_010_VitalSignRoutesTest {
    @Test
    void FR_010_englishRoutesBindResidentAndRecordIds() throws Exception {
        var measurements = mock(VitalSignMeasurementService.class);
        var user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(9L);
        var now = Instant.parse("2030-01-01T00:00:00Z");
        var record = new VitalSignRecord(
                1L,
                42L,
                2L,
                now,
                VitalSignLevel.NORMAL,
                VitalSignConfirmationStatus.CONFIRMED,
                Map.of(),
                Set.of(),
                now);
        when(measurements.getHistory(42L, null, null, null)).thenReturn(List.of());
        when(measurements.record(any(), same(user))).thenReturn(record);
        when(measurements.remeasure(any(), same(user))).thenReturn(record);
        var http = MockMvcBuilders.standaloneSetup(new VitalSignController(measurements, user))
                .build();

        http.perform(get("/api/v1/residents/42/vital-signs")).andExpect(status().isOk());
        http.perform(post("/api/v1/residents/42/vital-signs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indicatorId\":2,\"values\":{\"VALUE\":98}}"))
                .andExpect(status().isCreated());
        http.perform(post("/api/v1/vital-sign-records/1/remeasure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"VALUE\":98}}"))
                .andExpect(status().isCreated());
        verify(measurements).getHistory(42L, null, null, null);
        verify(measurements).record(argThat(command -> command.residentId() == 42L), same(user));
        verify(measurements).remeasure(argThat(command -> command.originalRecordId() == 1L), same(user));
        http.perform(get("/api/v1/nguoi-cao-tuoi/42/chi-so")).andExpect(status().isNotFound());
        http.perform(post("/api/v1/ban-ghi-chi-so/1/do-lai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
