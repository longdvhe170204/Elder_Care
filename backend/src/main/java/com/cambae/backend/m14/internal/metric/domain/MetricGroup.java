package com.cambae.backend.m14.internal.metric.domain;

/**
 * Nhóm chỉ tiêu của bảng FR-010 ({@code nhom_chi_tieu}); tên là giá trị cột {@code metric_group} của thu hẹp quyền
 * 002 (FR-010a). Chú thích là dòng của bảng.
 */
public enum MetricGroup {
    /** Người cao tuổi: số lượng, trạng thái, loại lưu trú, mức chăm sóc, biến động, tạm vắng. */
    RESIDENTS,
    WAITING_LIST,
    /** Phòng, giường theo trạng thái. */
    BEDS,
    /** Công việc chăm sóc (trạng thái, ca, tầng, ghi nhận muộn). */
    CARE_TASKS,
    /** Chỉ tiêu theo từng nhân viên (công việc, kiểm tra chất lượng). */
    PER_STAFF,
    /** Thời gian giường Chờ sẵn sàng (Q-282). */
    BED_AWAITING_READY,
    /** Phiếu bữa ăn giao trễ, có sai lệch. */
    MEAL_TICKETS,
    /** Hoạt động, tỷ lệ tham gia, chuyến đi, nguy cơ cô lập. */
    ACTIVITIES,
    QUALITY_CHECKS,
    /** Chỉ số, cảnh báo, sự cố, trường hợp cần theo dõi. */
    HEALTH_EVENTS,
    /** Sự cố thuộc loại mang dấu "không thuộc sức khỏe" (FR-042a). */
    NON_HEALTH_INCIDENTS,
    OVERDUE_REASSESSMENTS,
    /** Liều thuốc (tỷ lệ Bỏ lỡ, Từ chối). */
    MEDICATION_DOSES,
    /** Phiếu đối chiếu thuốc quá hạn. */
    MEDICATION_RECONCILIATIONS,
    CONTAINMENT_ZONES,
    /** Bàn giao chưa xác nhận. */
    HANDOVERS,
    /** Tỷ lệ phục vụ ca hiện tại. */
    STAFFING_RATIO,
    FACILITY_LICENSES,
    /** Giấy phép hành nghề, chứng chỉ đào tạo nhân viên sắp hết hạn. */
    STAFF_LICENSES,
    INCURRED_COSTS,
    /** Tổng chi mua sắm theo bộ phận, theo trạng thái (Q-284). */
    PURCHASING,
    /** Số dư, thu chi: Sắp hết tiền, Còn nợ (017). */
    BALANCES,
    /** Khai báo tạm trú cần làm (018). */
    RESIDENCE_REGISTRATION,
    /** Tài sản quá hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm (019). */
    ASSETS,
    /** Người đang mang dấu nguy kịch (007). */
    CRITICAL_FLAGS,
    /** Nhân sự và ca trực (mục I). */
    STAFFING_SHIFTS
}
