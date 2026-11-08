package com.eldercare.backend.staffing.internal.swap.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider của 015 (T008): phạm vi của yêu cầu đổi ca, nghỉ đột xuất theo tầng/khu vực của ca hoặc phạm vi nghỉ (002 FR-034)
 * và phân loại trường lý do là {@code STAFF_REASON} (FR-045, BR-M15-11).
 */
@Configuration(proxyBeanMethods = false)
class SwapGovernanceConfig {

    private static final String SWAP = "M09.SHIFT_SWAP";
    private static final String LEAVE = "M09.LEAVE_REQUEST";

    private static ScopeAssignmentProvider scope(String type, ApprovalRequestApi approvals) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return type;
            }

            @Override
            public Target assignScope(long id) {
                var request = approvals.find(id).orElseThrow(NotFoundException::new);
                return new Target(type, Long.toString(id), null, request.target().floorAreaId());
            }
        };
    }

    private static FieldClassificationProvider reasons(String type, String... fields) {
        Map<String, InfoGroup> groups = new java.util.LinkedHashMap<>();
        for (String f : fields) {
            groups.put(f, InfoGroup.STAFF_REASON);
        }
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return type;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.copyOf(groups);
            }
        };
    }

    @Bean
    ScopeAssignmentProvider swapScope(ApprovalRequestApi approvals) {
        return scope(SWAP, approvals);
    }

    @Bean
    ScopeAssignmentProvider leaveScope(ApprovalRequestApi approvals) {
        return scope(LEAVE, approvals);
    }

    @Bean
    FieldClassificationProvider swapFields() {
        return reasons(SWAP, "reason", "recipientDeclineReason");
    }

    @Bean
    FieldClassificationProvider leaveFields() {
        return reasons(LEAVE, "reason", "rejectReason");
    }
}
