package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.ReassessmentState;
import com.cambae.backend.m01.internal.assessment.service.ReassessmentQueryService.Entry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Trạng thái đánh giá lại và các căn cứ đã báo (contracts/api-001.yaml, schema {@code ReassessmentStatus}). */
@Schema(name = "ReassessmentStatus")
public record ReassessmentBody(
        long residentId, ReassessmentState status, Instant dueAt, Instant overdueSince, List<BasisBody> basis) {

    /** Một căn cứ đã báo; {@code source} có dạng {@code LOẠI:mã} (ASSESSMENT, INCIDENT, STATUS_HISTORY, CORRECTION). */
    public record BasisBody(String basis, String source, Instant notifiedAt) {}

    public static ReassessmentBody of(Entry entry) {
        var status = entry.detail().status();
        return new ReassessmentBody(
                entry.residentId(),
                status.state(),
                status.dueAt().orElse(null),
                status.overdueSince().orElse(null),
                entry.detail().basis().stream()
                        .map(n -> new BasisBody(n.basis().name(), n.sourceType() + ":" + n.sourceId(), n.notifiedAt()))
                        .toList());
    }
}
