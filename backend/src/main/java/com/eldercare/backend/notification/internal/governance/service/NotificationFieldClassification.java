package com.eldercare.backend.notification.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường của thông báo khi xem nhật ký ({@code PhanLoaiTruongProvider}; data-model mục 9, research B-13):
 * phần nội dung theo loại thông tin ({@code content.<loại>}), địa chỉ gửi (số điện thoại) là liên hệ. Loại thông tin
 * không có nhóm tương ứng là công khai nội bộ.
 */
@Component
class NotificationFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return "M13.NOTIFICATION";
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of(
                "content.HEALTH", InfoGroup.HEALTH,
                "content.COSTS", InfoGroup.COSTS,
                "address", InfoGroup.CONTACT);
    }
}
