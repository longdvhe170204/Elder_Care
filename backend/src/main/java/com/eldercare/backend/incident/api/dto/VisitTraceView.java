package com.eldercare.backend.incident.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Lượt Đã vào hoặc Đã ra phục vụ truy vết; exitedAt rỗng khi chưa ghi giờ ra (FR-060). */
public record VisitTraceView(
        long visitId, Instant enteredAt, Optional<Instant> exitedAt, List<VisitTracePersonView> people) {
    public VisitTraceView {
        people = List.copyOf(people);
    }
}
