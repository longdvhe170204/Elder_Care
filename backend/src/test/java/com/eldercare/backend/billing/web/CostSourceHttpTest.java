package com.eldercare.backend.billing.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.source.domain.CostSourceCandidate;
import com.eldercare.backend.billing.internal.source.service.*;
import com.eldercare.backend.common.error.web.ProblemDetailsHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP thật của controller/validation; không thay test quyền hoặc transaction MySQL. */
class CostSourceHttpTest {
    private final CostSourceLookup lookup = mock(CostSourceLookup.class);
    private final SourceCostCommands commands = mock(SourceCostCommands.class);
    private final CostAccess access = mock(CostAccess.class);
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new CostSourceController(lookup, commands, access))
            .setControllerAdvice(new ProblemDetailsHandler())
            .build();

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"residentId\":42,\"sourceType\":\"DOSE\",\"sourceIds\":[\"1\"],\"dateRange\":{}}",
                "{\"residentId\":42,\"sourceType\":\"DOSE\",\"sourceUnits\":[null]}",
                "{\"residentId\":42,\"sourceType\":\"DOSE\",\"sourceIds\":[\"1\",\"1\"]}"
            })
    void FR_005_invalidSelectionNeverReachesCommand(String body) throws Exception {
        mvc.perform(post("/api/v1/costs/items/from-events")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @Test
    void FR_005_optionalSourceFieldsAreOmittedInsteadOfReturningInvalidNulls() throws Exception {
        var date = LocalDate.of(2026, 11, 1);
        when(lookup.find(42, "DOSE", date, date))
                .thenReturn(List.of(new CostSourceCandidate(
                        42,
                        "DOSE",
                        "1",
                        "2",
                        date,
                        null,
                        null,
                        null,
                        null,
                        "Bản ghi tính phí",
                        BigDecimal.ONE,
                        null,
                        Set.of(),
                        null,
                        "CONDITION_NOT_MET",
                        "Thiếu metadata",
                        Map.of())));
        mvc.perform(get("/api/v1/costs/billing-sources")
                        .param("residentId", "42")
                        .param("type", "DOSE")
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].billingUnit").value("2"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"unit\":null"))));
    }
}
