package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 007 T108 (FR-081; 016 FR-042, FR-042a; "Đổi api" duyệt 2026-11-08): dòng sự cố cho 016 có mức lúc tiếp nhận (giữ
 * nguyên khi mức đổi sau đó; rỗng khi chưa tiếp nhận) và trạng thái hiện hành.
 */
class FR_081_IncidentReportIT extends IncidentHttpTestBase {

    @Autowired
    IncidentStatisticsApi statistics;

    @Test
    void FR_081_levelAtAcknowledgement_keptAfterLevelChange_andStatusIsCurrent() throws Exception {
        long received = incident("OTHER_INJURY", "LOW", "NEW", null, nurse.accountId());
        long waiting = incident("OTHER_INJURY", "MEDIUM", "NEW", null, nurse.accountId());
        post(floorManager, "/api/v1/incidents/{id}/receive", received, "{}").andExpect(status().isOk());
        post(
                        floorManager,
                        "/api/v1/incidents/{id}/change-level",
                        received,
                        "{\"newLevel\":\"MEDIUM\",\"reason\":\"Đau tăng\"}")
                .andExpect(status().isOk());

        Map<Long, IncidentReportView> rows = statistics
                .incidents(
                        new ScopeFilter(true, Set.of(), Set.of(), Set.of()),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(1)))
                .collect(Collectors.toMap(IncidentReportView::incidentId, Function.identity()));

        assertThat(rows.get(received).level()).isEqualTo("MEDIUM");
        assertThat(rows.get(received).levelAtAcknowledgement()).isEqualTo("LOW");
        assertThat(rows.get(received).status()).isEqualTo("IN_PROGRESS");
        assertThat(rows.get(waiting).levelAtAcknowledgement()).isNull();
        assertThat(rows.get(waiting).status()).isEqualTo("NEW");
    }
}
