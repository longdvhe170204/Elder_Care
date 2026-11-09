package com.eldercare.backend.billing.internal.purchase.config;

import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import java.util.Map;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
class PurchaseGovernance {
    @Bean
    ScopeAssignmentProvider purchaseScope(SimpleRecordApi records) {
        return new ScopeAssignmentProvider() {
            public String entityType() {
                return PurchaseDetails.KIND.code();
            }

            public Target assignScope(long id) {
                var row = records.require(PurchaseDetails.KIND, id);
                return new Target(entityType(), Long.toString(id), row.residentId(), null);
            }
        };
    }

    @Bean
    FieldClassificationProvider purchaseFields() {
        return new FieldClassificationProvider() {
            public String entityType() {
                return PurchaseDetails.KIND.code();
            }

            public Map<String, InfoGroup> fieldGroups() {
                return Map.of(
                        "content",
                        InfoGroup.FINANCE,
                        "status",
                        InfoGroup.FINANCE,
                        "description",
                        InfoGroup.FINANCE,
                        "estimatedAmount",
                        InfoGroup.FINANCE,
                        "allowedAmount",
                        InfoGroup.FINANCE,
                        "actualAmount",
                        InfoGroup.FINANCE,
                        "reason",
                        InfoGroup.FINANCE,
                        "medicationName",
                        InfoGroup.MEDICATION);
            }
        };
    }
}
