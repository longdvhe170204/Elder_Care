package com.eldercare.backend.billing.internal.provision.service;

import com.eldercare.backend.admission.api.*;
import com.eldercare.backend.billing.api.dto.CostTypeTotal;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.pricing.domain.ResidencyDayFee;
import com.eldercare.backend.billing.internal.provision.domain.*;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CostForBalance implements CostForBalancePort {
    private final CostSummaries summaries;
    private final CostStatementStore statements;
    private final FinalizedCostViews views;
    private final CostProvisionStore store;
    private final ContractReadApi contracts;
    private final UnitPriceApi prices;

    public CostForBalance(
            CostSummaries summaries,
            CostStatementStore statements,
            FinalizedCostViews views,
            CostProvisionStore store,
            ContractReadApi contracts,
            UnitPriceApi prices) {
        this.summaries = summaries;
        this.statements = statements;
        this.views = views;
        this.store = store;
        this.contracts = contracts;
        this.prices = prices;
    }

    public long unfinalizedTotal(long resident) {
        return summaries.provisional(resident, null, null).totalsByType().stream()
                .map(CostTypeTotal::total)
                .reduce(0L, Math::addExact);
    }

    public Optional<Long> baseDailyFee(long resident, LocalDate date) {
        return contracts.contentAtDate(resident, date).flatMap(c -> {
            if (!"MONTH".equals(c.content().path("residencyFeeBillingPeriod").asString())
                    || !c.content().path("roomTypeId").isNumber()) return Optional.empty();
            return prices.residencyFeeServiceOf(c.content().path("roomTypeId").asLong())
                    .flatMap(service ->
                            Optional.ofNullable(c.recordedUnitPrices().get(service.id())))
                    .map(price -> ResidencyDayFee.monthly(price, date).longValueExact());
        });
    }

    public Optional<FinalizedCostStatement> finalizedStatement(long id) {
        var s = statements.statement(id);
        return s.status().equals("FINALIZED")
                ? Optional.of(new FinalizedCostStatement(s.periodId(), store.finalizedAt(id), views.statement(s)))
                : Optional.empty();
    }
}
