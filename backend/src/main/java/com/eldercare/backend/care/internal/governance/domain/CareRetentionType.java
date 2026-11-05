package com.eldercare.backend.care.internal.governance.domain;
/** T008: tách loại lưu giữ theo hồ sơ/vị trí vì SPI chung có một RetentionKind mỗi dataType. */
public enum CareRetentionType {
    CARE_PLAN("care_plan", true),
    CARE_PLAN_ITEM("care_plan_item", true),
    RISK_AGREEMENT("care_plan_item", true),
    // Con record_event được SharedRowRetention của 000 loại bỏ cùng thỏa thuận, không quét độc lập.
    RISK_OPINION("record_event", true),
    TASK("care_task", true),
    LOCATION_TASK("care_task", false),
    CARE_RESULT("care_task", true),
    LOCATION_CARE_RESULT("care_task", false),
    TASK_HISTORY("care_task_history", true),
    LOCATION_TASK_HISTORY("care_task_history", false),
    // Điểm danh đã gộp vào simple_record, record_event gắn M01.RESIDENT (R-15): job loại bỏ xóa cùng chuỗi hồ sơ và xét
    // đính chính của từng dòng qua SharedRowRetention.rowsFor; hai loại dưới chỉ giữ khai báo, bảng cũ không còn nên
    // rỗng.
    DAY_CARE_ATTENDANCE("care_day_attendance", true),
    ATTENDANCE_RECORD("care_attendance_record", true),
    // 014 T006 (research L-15): buổi, chuyến không gắn hồ sơ (mốc là giờ bắt đầu); đăng ký theo hồ sơ. Dòng 014 ở bảng
    // chung (sở thích, người thường xuyên, điểm danh, kết quả kiểm tra) gắn resident_id, xóa cùng hồ sơ qua
    // SharedRowRetention.
    ACTIVITY_SESSION("activity_session", false),
    ACTIVITY_REGISTRATION("activity_registration", true);
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
