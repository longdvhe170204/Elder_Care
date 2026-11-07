package com.eldercare.backend.family.internal.visit.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider của lượt thăm (012 T008, T024): phạm vi theo người cao tuổi ({@code GanPhamViProvider} {@code M10.LUOT_THAM});
 * người đi cùng có số điện thoại là thông tin liên hệ. Lượt gắn hồ sơ nên loại bỏ cùng chuỗi của bảng chung.
 */
@Configuration
class VisitConfig {

    @Bean
    ScopeAssignmentProvider visitScope(SimpleRecordApi records) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return Visit.KIND.code();
            }

            @Override
            public Target assignScope(long id) {
                return new Target(
                        Visit.KIND.code(),
                        String.valueOf(id),
                        records.require(Visit.KIND, id).residentId(),
                        null);
            }
        };
    }

    @Bean
    FieldClassificationProvider visitFields() {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return Visit.KIND.code();
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.of("companions", InfoGroup.CONTACT);
            }
        };
    }
}
