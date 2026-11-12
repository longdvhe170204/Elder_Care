package com.eldercare.backend.reporting.internal.metric.domain;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import java.time.Instant;
import java.util.Map;

/**
 * Một dòng danh sách chi tiết ({@code DongChiTiet}; FR-009, FR-012; contracts {@code MetricDetail.rows}).
 *
 * @param resultGroup nhóm thông tin của {@code result} (ví dụ {@code HEALTH}, {@code MEDICATION}); rỗng là không giới
 *     hạn (FR-014)
 * @param restrictedResult phần của kết quả hiện cho người không được xem nhóm, ví dụ mã vật phẩm thay tên thuốc (Q-133)
 * @param reduced dạng rút gọn cho người cao tuổi ngoài phạm vi hiện hành (Q-196)
 */
public record DetailRow(
        String sourceKey,
        String feature,
        String type,
        String status,
        Instant referenceTime,
        Long floorAreaId,
        String floorArea,
        Long residentId,
        String resident,
        String performedBy,
        Map<String, Object> result,
        InfoGroup resultGroup,
        Map<String, Object> restrictedResult,
        boolean pendingReview,
        boolean reduced,
        boolean canOpenRecord) {

    /** Dòng của chỉ tiêu tổng hợp nguồn ngoài: kết quả không giới hạn nhóm, không chờ xem lại, mở được theo nguồn. */
    public static DetailRow simple(
            String sourceKey,
            String feature,
            String type,
            String status,
            Instant referenceTime,
            Long floorAreaId,
            Long residentId,
            Map<String, Object> result) {
        return new DetailRow(
                sourceKey,
                feature,
                type,
                status,
                referenceTime,
                floorAreaId,
                null,
                residentId,
                null,
                null,
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }

    public DetailRow withResult(Map<String, Object> shown) {
        return new DetailRow(
                sourceKey,
                feature,
                type,
                status,
                referenceTime,
                floorAreaId,
                floorArea,
                residentId,
                resident,
                performedBy,
                shown,
                resultGroup,
                restrictedResult,
                pendingReview,
                reduced,
                canOpenRecord);
    }

    /** Q-196: không mở được bản ghi từ danh sách (mở hồ sơ hiện tại cũng không). */
    /** Mở được bản ghi gốc ở màn của feature sở hữu (FR-009, Q12). */
    public DetailRow withCanOpenRecord(boolean open) {
        return new DetailRow(
                sourceKey,
                feature,
                type,
                status,
                referenceTime,
                floorAreaId,
                floorArea,
                residentId,
                resident,
                performedBy,
                result,
                resultGroup,
                restrictedResult,
                pendingReview,
                reduced,
                open);
    }

    public DetailRow asReduced() {
        return new DetailRow(
                sourceKey,
                feature,
                type,
                status,
                referenceTime,
                floorAreaId,
                floorArea,
                residentId,
                resident,
                performedBy,
                result,
                resultGroup,
                restrictedResult,
                pendingReview,
                true,
                false);
    }
}
