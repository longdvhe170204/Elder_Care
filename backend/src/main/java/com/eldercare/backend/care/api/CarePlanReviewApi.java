package com.eldercare.backend.care.api;

import java.time.Instant;

/**
 * BR-M04-20, FR-011 (tên logic {@code BaoXemXetKeHoachApi}; chủ dự án duyệt 08/11, xác nhận 09/11/2026): 007 gọi trong
 * transaction của lệnh ghi (MANDATORY) để 005 báo xem xét kế hoạch chăm sóc; 005 không nghe sự kiện của 007 (vòng module,
 * research C-02). Lệnh chỉ phát {@code CarePlanReviewNeeded}, không tạo bản ghi nghiệp vụ (Q35); cùng khóa nguồn gọi lặp
 * không báo lần hai. Lỗi ném ra làm lệnh 007 rollback.
 */
public interface CarePlanReviewApi {

    /** Sự cố ngã đã ghi ({@code SuCoNgaDaGhiNhan}). */
    void fallIncidentRecorded(long residentId, long incidentId);

    /** Lựa chọn ở lại chăm sóc giảm nhẹ đã ghi ({@code LuaChonChamSocGiamNheDaGhi}); {@code eventKey} ổn định theo lần chọn. */
    void palliativeCareChoiceRecorded(long residentId, String eventKey, Instant occurredAt);
}
