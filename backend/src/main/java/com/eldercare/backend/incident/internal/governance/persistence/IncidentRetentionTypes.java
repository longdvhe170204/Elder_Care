package com.eldercare.backend.incident.internal.governance.persistence;

/**
 * Loại dữ liệu lưu giữ của M05 (data-model 007 mục 14). Sự cố, cảnh báo không gắn hồ sơ tách loại để tính hạn
 * CFG-M15-04 nhưng dòng bảng chung vẫn gắn {@link #INCIDENT}, {@link #ALERT}.
 */
public final class IncidentRetentionTypes {
    public static final String INCIDENT = "M05.INCIDENT";
    public static final String FACILITY_INCIDENT = "M05.FACILITY_INCIDENT";
    public static final String ALERT = "M05.ALERT";
    public static final String UNLINKED_ALERT = "M05.UNLINKED_ALERT";

    private IncidentRetentionTypes() {}
}
