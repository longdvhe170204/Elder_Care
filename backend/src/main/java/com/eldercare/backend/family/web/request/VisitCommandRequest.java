package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.family.web.request.VisitRegistrationRequest.CompanionBody;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Thân lệnh trên lượt thăm: lý do (hủy của Hành chính), người thực tế vào (ghi vào, FR-033), giờ ra thực tế (ghi ra,
 * FR-034; trống là lúc ghi).
 */
public record VisitCommandRequest(
        Reason reason, @Size(max = 20) List<CompanionBody> actualVisitors, Instant checkOutAt) {

    public static final VisitCommandRequest EMPTY = new VisitCommandRequest(null, null, null);
}
