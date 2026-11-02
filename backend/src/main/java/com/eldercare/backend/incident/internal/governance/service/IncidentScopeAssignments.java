package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gắn phạm vi cho đối tượng M05 (002 FR-034).
 */
@Configuration
public class IncidentScopeAssignments {

    public static final String INCIDENT = "M05.INCIDENT";
    public static final String CONTAINMENT = "M05.CONTAINMENT";

    @Bean
    public ScopeAssignmentProvider incidentScopeAssignment() {
        return incident();
    }

    @Bean
    public ScopeAssignmentProvider containmentScopeAssignment() {
        return containment();
    }

    public static ScopeAssignmentProvider incident() {
        return new ScopeAssignment(INCIDENT, (id) -> new Target(INCIDENT, String.valueOf(id), null, null));
    }

    public static ScopeAssignmentProvider containment() {
        return new ScopeAssignment(CONTAINMENT, (id) -> new Target(CONTAINMENT, String.valueOf(id), null, null));
    }

    record ScopeAssignment(String entityType, java.util.function.LongFunction<Target> mapper)
            implements ScopeAssignmentProvider {
        @Override
        public Target assignScope(long id) {
            return mapper.apply(id);
        }
    }
}
