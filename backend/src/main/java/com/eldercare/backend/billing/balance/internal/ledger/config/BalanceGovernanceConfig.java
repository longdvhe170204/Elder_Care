package com.eldercare.backend.billing.balance.internal.ledger.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceGovernanceStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceRetention;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceScopeAssignment;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 017 T007 (research E-12, data-model mục 8): phân loại trường, lưu giữ, phạm vi của sổ và giao dịch. Mọi trường tiền,
 * mã nộp tiền, thông tin nộp tiền thuộc nhóm tài chính; Hành chính chỉ thấy số dư, số dư cọc, trạng thái đặt cọc do
 * endpoint của 017 lọc (FR-004).
 */
@Configuration(proxyBeanMethods = false)
class BalanceGovernanceConfig {

    private static FieldClassificationProvider financialFields(String type, Set<String> fields) {
        Map<String, InfoGroup> groups =
                fields.stream().collect(Collectors.toUnmodifiableMap(field -> field, field -> InfoGroup.FINANCE));
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return type;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return groups;
            }
        };
    }

    @Bean
    FieldClassificationProvider balanceLedgerFields() {
        return financialFields(
                BalanceTypes.LEDGER,
                Set.of(
                        "paymentCode",
                        "balance",
                        "depositBalance",
                        "debt",
                        "daysCovered",
                        "unfinalizedTotal",
                        "paymentInstructions",
                        "remindersStopped",
                        "reason"));
    }

    @Bean
    FieldClassificationProvider balanceTransactionFields() {
        return financialFields(
                BalanceTypes.TRANSACTION,
                Set.of(
                        "amount",
                        "method",
                        "bankTransactionCode",
                        "costStatementId",
                        "payerName",
                        "recipient",
                        "note",
                        "reason"));
    }

    @Bean
    RetentionProvider balanceLedgerRetention(BalanceRetention retention) {
        return retention.provider(BalanceRetention.Kind.LEDGER);
    }

    @Bean
    RetentionProvider balanceTransactionRetention(BalanceRetention retention) {
        return retention.provider(BalanceRetention.Kind.TRANSACTION);
    }

    @Bean
    ScopeAssignmentProvider balanceLedgerScope(BalanceGovernanceStore store) {
        return new BalanceScopeAssignment(BalanceTypes.LEDGER, store);
    }

    @Bean
    ScopeAssignmentProvider balanceTransactionScope(BalanceGovernanceStore store) {
        return new BalanceScopeAssignment(BalanceTypes.TRANSACTION, store);
    }
}
