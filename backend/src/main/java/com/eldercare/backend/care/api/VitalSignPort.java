package com.eldercare.backend.care.api;

import com.eldercare.backend.administration.account.api.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * SPI khai báo bởi module M04 (005), cài bởi module M06 (007 health).
 * Cho phép module 005 ghi nhận chỉ số khi hoàn thành công việc đo theo lịch (BR-M04-10, S-06).
 */
public interface VitalSignPort {

    /**
     * Ghi nhận bản đo chỉ số từ một công việc đo theo lịch.
     *
     * @param residentId  mã người cao tuổi
     * @param indicatorId mã danh mục chỉ số
     * @param measuredAt  thời điểm đo
     * @param taskId      mã công việc đo
     * @param values      giá trị từng thành phần (mã thành phần → giá trị)
     * @param user        người thực hiện ghi
     * @return kết quả ghi: {@code SAVED} đã lưu thành công (giá trị bình thường hoặc cảnh báo),
     *     {@code BLOCKED} bị chặn ngoài khoảng vật lý CFG-M06-03,
     *     {@code AWAITING_REMEASUREMENT} đã lưu nhưng giá trị nguy hiểm chờ đo lại (BR-M06-04)
     */
    RecordResult record(
            long residentId,
            long indicatorId,
            Instant measuredAt,
            long taskId,
            Map<String, BigDecimal> values,
            CurrentUser user);

    /** Kết quả ghi chỉ số từ công việc đo lịch (S-06, FR-034, BR-M04-10). */
    enum RecordResult {
        /** Đã lưu, không có vấn đề nghiệp vụ. */
        SAVED,
        /** Giá trị nằm ngoài khoảng vật lý CFG-M06-03, bị chặn không lưu. */
        BLOCKED,
        /** Đã lưu nhưng ở trạng thái nguy hiểm, cần đo lại (BR-M06-04). */
        AWAITING_REMEASUREMENT
    }
}
