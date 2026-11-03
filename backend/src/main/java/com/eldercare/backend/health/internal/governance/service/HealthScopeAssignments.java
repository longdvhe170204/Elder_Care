package com.eldercare.backend.health.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gắn phạm vi cho đối tượng M06 (002 FR-034).
 */
@Configuration
public class HealthScopeAssignments {

    public static final String VITAL_SIGN_RECORD = "M06.VITAL_SIGN_RECORD";
    public static final String FACILITY_LICENSE = "M06.FACILITY_LICENSE";

    @Bean
    public ScopeAssignmentProvider vitalSignScopeAssignment() {
        return vitalSignRecord();
    }

    @Bean
    public ScopeAssignmentProvider facilityLicenseScopeAssignment() {
        return facilityLicense();
    }

    public static ScopeAssignmentProvider vitalSignRecord() {
        return new ScopeAssignment(
                VITAL_SIGN_RECORD, (id) -> new Target(VITAL_SIGN_RECORD, String.valueOf(id), null, null));
    }

    public static ScopeAssignmentProvider facilityLicense() {
        return new ScopeAssignment(
                FACILITY_LICENSE, (id) -> new Target(FACILITY_LICENSE, String.valueOf(id), null, null));
    }

    record ScopeAssignment(String entityType, java.util.function.LongFunction<Target> mapper)
            implements ScopeAssignmentProvider {
        @Override
        public Target assignScope(long id) {
            return mapper.apply(id);
        }
    }
}
