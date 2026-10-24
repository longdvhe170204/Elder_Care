package com.cambae.backend.m14.internal.metric.domain;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
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
