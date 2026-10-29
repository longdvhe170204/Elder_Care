package com.cambae.backend.m09.shift.internal.service.port;

import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.List;

/** 008 N-07: chỉ swap của 015 gọi; mọi hàm MANDATORY, không tự kiểm quyền thay lệnh 015. */
public interface ShiftCommandPort {
    List<PublicationWarning> addToShift(long shiftId, long staffId, ShiftEntryBasis basis, boolean confirmWarnings);

    void leaveShift(long shiftId, long staffId, ShiftDepartureBasis basis);

    void changeLead(long shiftId, long staffId, Basis basis);

    void markMissingLead(long shiftId, Basis basis);
}
