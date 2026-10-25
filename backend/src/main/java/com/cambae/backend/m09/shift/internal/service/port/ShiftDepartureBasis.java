package com.cambae.backend.m09.shift.internal.service.port;

import com.cambae.backend.m15.auditlog.api.Basis;

/** Căn cứ rời ca của 015; sourceId của APPROVED_LEAVE là yêu cầu nghỉ được duyệt. */
public record ShiftDepartureBasis(Kind kind, long sourceId) {
    public enum Kind {
        SWAPPED,
        APPROVED_LEAVE
    }

    public ShiftDepartureBasis {
        if (kind == null || sourceId <= 0) throw new IllegalArgumentException("Thiếu căn cứ rời ca");
    }

    public Basis auditBasis() {
        return new Basis(null, null, sourceId, null);
    }
}
