package com.eldercare.backend.billing.internal.governance.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.billing.internal.governance.persistence.CostRetention;
import com.eldercare.backend.billing.internal.governance.persistence.CostScopeStore;
import com.eldercare.backend.billing.internal.governance.service.CostScopeAssignment;
import java.util.Map;
import org.springframework.context.annotation.*;

/** T008: đăng ký metadata cho các bảng 010 hiện có; không mở quyền hoặc tạo provider giả cho dữ liệu tương lai. */
@Configuration(proxyBeanMethods = false)
class M11GovernanceConfig {
    @Bean
    RetentionProvider costPeriodRetention(CostRetention store) {
        return store.provider(CostRetention.Kind.PERIOD);
    }

    @Bean
    RetentionProvider costStatementRetention(CostRetention store) {
        return store.provider(CostRetention.Kind.STATEMENT);
    }

    @Bean
    RetentionProvider costItemRetention(CostRetention store) {
        return store.provider(CostRetention.Kind.ITEM);
    }

    @Bean
    RetentionProvider costSourceIndexRetention(CostRetention store) {
        return store.provider(CostRetention.Kind.SOURCE_INDEX);
    }

    private static FieldClassificationProvider financialFields(String type) {
        return new FieldClassificationProvider() {
            public String entityType() {
                return type;
            }

            public Map<String, InfoGroup> fieldGroups() {
                return Map.ofEntries(
                        Map.entry("amount", InfoGroup.FINANCE),
                        Map.entry("quantity", InfoGroup.FINANCE),
                        Map.entry("unitPrice", InfoGroup.FINANCE),
                        Map.entry("factorPercent", InfoGroup.FINANCE),
                        Map.entry("total", InfoGroup.FINANCE),
                        Map.entry("totalsByType", InfoGroup.FINANCE),
                        Map.entry("rows", InfoGroup.FINANCE),
                        Map.entry("statementIds", InfoGroup.FINANCE),
                        Map.entry("firstStatementIds", InfoGroup.FINANCE),
                        Map.entry("reason", InfoGroup.FINANCE),
                        Map.entry("description", InfoGroup.FINANCE),
                        Map.entry("unitPriceBasis", InfoGroup.FINANCE),
                        Map.entry("sourceValues", InfoGroup.FINANCE),
                        Map.entry("before", InfoGroup.FINANCE),
                        Map.entry("after", InfoGroup.FINANCE),
                        Map.entry("medicationName", InfoGroup.MEDICATION));
            }
        };
    }

    @Bean
    FieldClassificationProvider costPeriodFields() {
        return financialFields("M11.COST_PERIOD");
    }

    @Bean
    FieldClassificationProvider costStatementFields() {
        return financialFields("M11.COST_STATEMENT");
    }

    @Bean
    FieldClassificationProvider costItemFields() {
        return financialFields("M11.COST_ITEM");
    }

    @Bean
    ScopeAssignmentProvider costStatementScope(CostScopeStore store) {
        return new CostScopeAssignment("M11.COST_STATEMENT", store);
    }

    @Bean
    ScopeAssignmentProvider costItemScope(CostScopeStore store) {
        return new CostScopeAssignment("M11.COST_ITEM", store);
    }
}
