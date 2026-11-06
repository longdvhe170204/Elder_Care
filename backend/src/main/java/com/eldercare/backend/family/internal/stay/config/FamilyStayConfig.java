package com.eldercare.backend.family.internal.stay.config;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provider của lượt ở lại (012 T035): phạm vi theo người cao tuổi ({@code GanPhamViProvider} {@code M10.LUOT_O_LAI}). */
@Configuration
class FamilyStayConfig {

    @Bean
    ScopeAssignmentProvider familyStayScope(SimpleRecordApi records) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return FamilyStay.KIND.code();
            }

            @Override
            public Target assignScope(long id) {
                return new Target(
                        FamilyStay.KIND.code(),
                        String.valueOf(id),
                        records.require(FamilyStay.KIND, id).residentId(),
                        null);
            }
        };
    }
}
