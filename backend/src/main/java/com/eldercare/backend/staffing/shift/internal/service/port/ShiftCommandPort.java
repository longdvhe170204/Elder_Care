package com.eldercare.backend.staffing.shift.internal.service.port;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import java.util.List;

/** 008 N-07: chỉ swap của 015 gọi; mọi hàm MANDATORY, không tự kiểm quyền thay lệnh 015. */
public interface ShiftCommandPort {
    List<PublicationWarning> addToShift(long shiftId, long staffId, ShiftEntryBasis basis, boolean confirmWarnings);

    void leaveShift(long shiftId, long staffId, ShiftDepartureBasis basis);

    void changeLead(long shiftId, long staffId, Basis basis);

    void markMissingLead(long shiftId, Basis basis);
}
