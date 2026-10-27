package com.cambae.backend.m02.internal.registration.config;

import com.cambae.backend.m02.internal.registration.service.RegistrationNames;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhật ký của khai báo ({@code PhanLoaiTruongProvider}; contracts/module-interfaces.md mục 2; FR-049 của
 * 000): spec, data-model 018 không xếp trường nào của khai báo vào nhóm sức khỏe, tài chính hay nhóm hạn chế khác, nên mọi
 * trường là công khai nội bộ; xem lịch sử vẫn theo quyền dòng "Khai báo tạm trú, lưu trú" (Hành chính, Quản lý viện; FR-009).
 * Có provider để vai trò có quyền xem lịch sử, không chỉ Quản lý viện (BR-M15-11).
 */
@Configuration
public class RegistrationFieldClassificationConfig {

    @Bean
    FieldClassificationProvider residenceRegistrationClassification() {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return RegistrationNames.ENTITY_TYPE;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.of();
            }
        };
    }
}
