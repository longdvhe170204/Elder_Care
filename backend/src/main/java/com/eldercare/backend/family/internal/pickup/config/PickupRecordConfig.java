package com.eldercare.backend.family.internal.pickup.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.family.internal.pickup.domain.PickupException;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider của quy trình đón (012 T008, T020): phạm vi của ngoại lệ đón theo người cao tuổi ({@code GanPhamViProvider}
 * {@code M10.NGOAI_LE_DON}); số giấy tờ của ngoại lệ, của bản ghi đón là dữ liệu định danh. Ngoại lệ, lần chặn, sự kiện
 * kết quả gắn hồ sơ hoặc bản ghi đón nên loại bỏ cùng chuỗi; bản ghi đón có {@code PickupRecordRetention}.
 */
@Configuration
class PickupRecordConfig {

    @Bean
    ScopeAssignmentProvider pickupExceptionScope(SimpleRecordApi records) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return PickupException.KIND.code();
            }

            @Override
            public Target assignScope(long id) {
                return new Target(
                        PickupException.KIND.code(),
                        String.valueOf(id),
                        records.require(PickupException.KIND, id).residentId(),
                        null);
            }
        };
    }

    @Bean
    FieldClassificationProvider pickupExceptionFields() {
        return fields(PickupException.KIND.code());
    }

    @Bean
    FieldClassificationProvider pickupRecordFields() {
        return fields(PickupRecord.ENTITY_TYPE);
    }

    private static FieldClassificationProvider fields(String entityType) {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return entityType;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.of("idDocumentNumber", InfoGroup.IDENTITY);
            }
        };
    }
}
