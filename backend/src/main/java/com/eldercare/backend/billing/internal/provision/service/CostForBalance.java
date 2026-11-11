package com.eldercare.backend.billing.internal.provision.service;

import com.eldercare.backend.admission.api.*;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.billing.api.dto.CostTypeTotal;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.pricing.domain.DongRounding;
import com.eldercare.backend.billing.internal.pricing.domain.ResidencyDayFee;
import com.eldercare.backend.billing.internal.provision.domain.*;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * 017 FR-016, Q-223: hợp đồng tháng: phí ngày cơ sở; hợp đồng ngày: đơn giá ngày; bán trú giá tháng: giá ÷ số ngày của
     * tháng; bán trú giá buổi: giá × số buổi có lịch trong 7 ngày tới ÷ 7. Làm tròn tới đồng; chưa có giá thì rỗng.
     */
    public Optional<Long> baseDailyFee(long resident, LocalDate date) {
        return contracts.contentAtDate(resident, date).flatMap(c -> {
            String period = c.content().path("residencyFeeBillingPeriod").asString();
            if ("DAY_CARE".equals(c.content().path("residenceType").asString()))
                return dayCare(resident, date, c, period);
            if (!c.content().path("roomTypeId").isNumber()) return Optional.empty();
            var price = prices.residencyFeeServiceOf(
                            c.content().path("roomTypeId").asLong())
                    .flatMap(service ->
                            Optional.ofNullable(c.recordedUnitPrices().get(service.id())));
            return switch (period) {
                case "MONTH" -> price.map(p -> ResidencyDayFee.monthly(p, date).longValueExact());
                case "DAY" -> price.map(p -> DongRounding.round(p).longValueExact());
                default -> Optional.empty();
            };
        });
    }

    private Optional<Long> dayCare(
            long resident,
            LocalDate date,
            com.eldercare.backend.admission.api.dto.ContractContentView c,
            String period) {
        Optional<BigDecimal> price = Optional.empty();
        for (var line : c.content().path("services")) {
            long id = line.path("serviceId").asLong();
            var service = prices.service(id);
            if (service.isPresent() && service.get().type() == ServiceType.DAY_CARE_SESSION) {
                price = Optional.ofNullable(c.recordedUnitPrices().get(id));
                break;
            }
        }
        return switch (period) {
            case "MONTH" ->
                price.map(p -> DongRounding.round(
                                p.divide(BigDecimal.valueOf(date.lengthOfMonth()), 6, RoundingMode.HALF_UP))
                        .longValueExact());
            case "SESSION" ->
                price.map(p -> {
                    int sessions = 0;
                    for (int i = 0; i < 7; i++)
                        sessions += contracts
                                .attendanceOn(resident, date.plusDays(i))
                                .size();
                    return DongRounding.round(p.multiply(BigDecimal.valueOf(sessions))
                                    .divide(BigDecimal.valueOf(7), 6, RoundingMode.HALF_UP))
                            .longValueExact();
                });
            default -> Optional.empty();
        };
    }

    public Optional<FinalizedCostStatement> finalizedStatement(long id) {
        var s = statements.statement(id);
        return s.status().equals("FINALIZED")
                ? Optional.of(new FinalizedCostStatement(s.periodId(), store.finalizedAt(id), views.statement(s)))
                : Optional.empty();
    }
}
