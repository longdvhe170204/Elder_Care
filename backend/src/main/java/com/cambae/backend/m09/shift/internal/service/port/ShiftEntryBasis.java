package com.cambae.backend.m09.shift.internal.service.port;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.auditlog.api.Basis;

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
