package com.eldercare.backend.incident.internal.alert.domain;

import java.util.Objects;

/**
 * Tiện ích tạo khóa loại cảnh báo theo quy định (FR-028, S-08, S-10).
 * Khóa loại dùng cho gộp cảnh báo mở (open_lock) theo từng người cao tuổi.
 */
public final class TypeLock {

    private TypeLock() {}

    /** Vượt ngưỡng chỉ số: {@code VUOT_NGUONG:<chiSoId>}. */
    public static String forVitalSign(long indicatorId) {
        return "VUOT_NGUONG:" + indicatorId;
    }

    /** Xu hướng: {@code XU_HUONG:<maQuyTac>}. */
    public static String forTrend(String ruleCode) {
        return "XU_HUONG:" + Objects.requireNonNull(ruleCode, "ruleCode");
    }

    /** Bỏ lỡ lần đo: {@code BO_LO_LAN_DO:<chiSoId>}. */
    public static String forMissedMeasurement(long indicatorId) {
        return "BO_LO_LAN_DO:" + indicatorId;
    }

    /** Công việc Bắt buộc quá hạn: {@code QUA_HAN:<loaiCongViecId>}. */
    public static String forOverdueTask(long taskTypeId) {
        return "QUA_HAN:" + taskTypeId;
    }

    /** Thuốc: {@code THUOC:<loai>:<id>}. */
    public static String forMedication(String type, long targetId) {
        return "THUOC:" + Objects.requireNonNull(type, "type") + ":" + targetId;
    }

    /** Thủ công: {@code THU_CONG:<loaiSuCoId>}. */
    public static String forManual(long incidentTypeId) {
        return "THU_CONG:" + incidentTypeId;
    }

    /** Nút SOS của người cao tuổi: {@code SOS}, gộp mọi lần bấm của một người khi còn mở (FR-028, S-23). */
    public static String forSos() {
        return "SOS";
    }

    /** Nút hỗ trợ khẩn cấp: {@code STAFF_ASSIST_REQUEST:<tài khoản người bấm>} (FR-047i, S-24). */
    public static String forStaffAssist(long accountId) {
        return "STAFF_ASSIST_REQUEST:" + accountId;
    }

    /** Dấu hiệu nguy kịch: {@code NGUY_KICH:<dauNguyKichId>}. */
    public static String forCritical(long criticalSignId) {
        return "NGUY_KICH:" + criticalSignId;
    }
}
