package com.eldercare.backend.administration.account.api.dto;

import com.eldercare.backend.common.Role;
import java.util.List;
import java.util.Map;

/**
 * Thu hẹp chi tiết đang hiệu lực của dòng "Dashboard, báo cáo" theo nhóm chỉ tiêu, kênh (016 FR-010a; 002 research
 * A-05). Thu hẹp mức khóa (không nhóm, không kênh) đã trừ trong {@code PermissionApi.permissions}, không có ở đây. Thứ tự
 * áp như A-05: thu hẹp của từng vai trò trước khi hợp các vai trò, thu hẹp theo tài khoản sau.
 */
public record MetricGroupRestrictions(Map<Role, List<Restriction>> byRole, List<Restriction> byAccount) {

    public static final MetricGroupRestrictions NONE = new MetricGroupRestrictions(Map.of(), List.of());

    /**
     * @param metricGroup mã nhóm chỉ tiêu của 016 ({@code MetricGroup}); rỗng là mọi nhóm của kênh
     * @param channel rỗng là cả dashboard và báo cáo
     */
    public record Restriction(String metricGroup, ReportChannel channel, Kind kind) {

        public boolean appliesTo(String group, ReportChannel ch) {
            return (metricGroup == null || metricGroup.equals(group)) && (channel == null || channel == ch);
        }
    }

    public enum Kind {
        /** Bỏ thao tác xem: nhóm không hiện. */
        REMOVE_OPERATION,
        /** Hạ phạm vi từ toàn viện xuống theo phân công. */
        LOWER_SCOPE
    }
}
