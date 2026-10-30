package com.eldercare.backend.staffing.shift.internal.service.port;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;

/** Căn cứ vào ca của 015; lý do được truyền từ lệnh gọi khi cần xác nhận cảnh báo. */
public record ShiftEntryBasis(Kind kind, long sourceId, Reason reason) {
    public enum Kind {
        SWAPPED,
        COVERED,
        INVITED
    }

    public ShiftEntryBasis {
        if (kind == null || sourceId <= 0) throw new IllegalArgumentException("Thiếu căn cứ vào ca");
    }

    public Basis auditBasis() {
        return new Basis(null, null, sourceId, null);
    }
}
