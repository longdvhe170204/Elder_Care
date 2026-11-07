package com.eldercare.backend.care.api;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 014 contracts mục 1.2 (ChuyenDiNhiemVuLenhApi): 015 gọi trong lệnh đổi ca, nghỉ đột xuất đã duyệt. */
@Transactional(propagation = Propagation.MANDATORY)
public interface TripDutyCommandApi {
    /**
     * FR-035: người rời ca bị gỡ trưởng đoàn, người đi cùng của chuyến chồng ca; báo Trưởng tầng phân công lại, không
     * chuyển cho người nhận ca.
     */
    void removeDuty(long staffId, long shiftId, Basis basis);
}
