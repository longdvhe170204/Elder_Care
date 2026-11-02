package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.waitinglist.service.WaitingListService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Danh sách hồ sơ phù hợp với một giường (contracts/api-004.yaml, schema {@code MatchingEntries}; FR-012, FR-013). */
@Schema(name = "MatchingEntries")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchingEntriesBody(long bedId, Instant vacancyStart, List<WaitingListEntryBody> entries) {

    public static MatchingEntriesBody of(WaitingListService.Matching matching) {
        return new MatchingEntriesBody(
                matching.bedId(),
                matching.vacancyStart(),
                matching.entries().stream().map(WaitingListEntryBody::of).toList());
    }
}
