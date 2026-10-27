package com.cambae.backend.m04.internal.governance.domain;
/** T008: tách loại lưu giữ theo hồ sơ/vị trí vì SPI chung có một RetentionKind mỗi dataType. */
public enum CareRetentionType {
    CARE_PLAN("care_plan", true),
    CARE_PLAN_ITEM("care_plan_item", true),
    RISK_AGREEMENT("care_risk_agreement", true),
    RISK_OPINION("care_risk_opinion", true),
    TASK("care_task", true),
    LOCATION_TASK("care_task", false),
    CARE_RESULT("care_result", true),
    LOCATION_CARE_RESULT("care_result", false),
    TASK_HISTORY("care_task_history", true),
    LOCATION_TASK_HISTORY("care_task_history", false),
    DAY_CARE_ATTENDANCE("care_day_attendance", true),
    ATTENDANCE_RECORD("care_attendance_record", true);
    private final String table;
    private final boolean profile;

    CareRetentionType(String table, boolean profile) {
        this.table = table;
        this.profile = profile;
    }

    public String dataType() {
        return "M04." + name();
    }

    public String table() {
        return table;
    }

    public boolean profile() {
        return profile;
    }
}
