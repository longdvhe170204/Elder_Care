package com.eldercare.backend.staffing.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;

/** Các khoảng phụ trách giữ loại đối tượng và id logic để lưu giữ/audit tra cùng bản ghi. */
public final class StaffingHistoryData {
    private StaffingHistoryData() {}

    public record LeadDetails(Legacy legacy) {}

    public static final HistoryTrack<LeadDetails> DEPARTMENT_LEAD =
            new HistoryTrack<>("M09.DEPARTMENT", "LEAD_STAFF", LeadDetails.class);
    public static final HistoryTrack<LeadDetails> SHIFT_LEAD =
            new HistoryTrack<>("M09.SHIFT", "LEAD_STAFF", LeadDetails.class);
}
