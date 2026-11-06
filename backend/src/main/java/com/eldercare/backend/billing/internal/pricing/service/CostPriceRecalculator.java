package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.admission.api.events.PriceVersionCreated;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.item.persistence.CostRepricingStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T014: cùng transaction cho giá, lịch sử, nhật ký, thông báo và dấu chống lặp; không lập khoản mới. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostPriceRecalculator {
    private final CostRepricingStore rows;
    private final CostLock locks;
    private final ExistingCostPricing pricing;
    private final ContractReadApi contracts;
    private final ReminderMarkers markers;
    private final StatusHistoryApi history;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public CostPriceRecalculator(
            CostRepricingStore rows,
            CostLock locks,
            ExistingCostPricing pricing,
            ContractReadApi contracts,
            ReminderMarkers markers,
            StatusHistoryApi history,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.rows = rows;
        this.locks = locks;
        this.pricing = pricing;
        this.contracts = contracts;
        this.markers = markers;
        this.history = history;
        this.audit = audit;
        this.time = time;
        this.events = events;
    }

    public void priceCreated(PriceVersionCreated event) {
        var key = "PRICE:" + event.serviceId() + ":" + event.effectiveFrom() + ":" + event.effectiveTo();
        process(
                rows.missingPrice(event.serviceId(), event.effectiveFrom(), event.effectiveTo()),
                key,
                "M02.SERVICE",
                event.serviceId(),
                null,
                event);
    }

    public void addendumApplied(AddendumApplied event) {
        process(
                rows.addendum(event.residentId(), event.effectiveDate()),
                "ADDENDUM:" + event.addendumId(),
                "M02.ADDENDUM",
                event.addendumId(),
                event,
                null);
    }

    private void process(
            List<RepriceCandidate> candidates,
            String causeKey,
            String causeType,
            long causeId,
            AddendumApplied addendum,
            PriceVersionCreated priceVersion) {
        var schedules = new HashMap<ExistingCostPricing.MonthKey, CalendarCostPricing.MonthSchedule>();
        for (var candidate : candidates) {
            locks.statement(candidate.residentId(), candidate.statementId());
            var locked = rows.lock(candidate.itemId(), candidate.residentId());
            if (locked.isEmpty()) continue;
            var row = locked.get();
            if (priceVersion != null
                    && (row.serviceId() != priceVersion.serviceId()
                            || !row.flags().contains("MISSING_UNIT_PRICE")
                            || row.incurredOn().isBefore(priceVersion.effectiveFrom())
                            || (priceVersion.effectiveTo() != null
                                    && row.incurredOn().isAfter(priceVersion.effectiveTo())))) continue;
            if (addendum != null) {
                boolean inRange = !row.incurredOn().isBefore(addendum.effectiveDate())
                        || (row.sourceType().equals("DAY_CARE_SESSION")
                                && !row.incurredOn()
                                        .isBefore(addendum.effectiveDate().withDayOfMonth(1)));
                var content = contracts.contentAtDate(row.residentId(), row.incurredOn());
                if (!inRange
                        || content.filter(value -> value.contractId() == addendum.contractId())
                                .isEmpty()) continue;
                boolean dayCare = "DAY_CARE"
                        .equals(content.get().content().path("residenceType").asString());
                if ((row.sourceType().equals("RESIDENCY_DAY") && dayCare)
                        || (row.sourceType().equals("DAY_CARE_SESSION") && !dayCare))
                    continue; // Đổi loại nguồn thuộc T021.
            }
            String key = "REPRICE:" + causeKey + ":" + row.id();
            if (!markers.mark(
                    "M11.SOURCE_PROCESSED",
                    "M11.PRICE_RECALCULATION",
                    row.residentId(),
                    key,
                    Map.of("itemId", row.id(), "basisType", causeType, "basisId", causeId))) continue;
            var calculated = pricing.calculate(row, schedules);
            if (calculated.isEmpty()) continue; // T021 xử lý hủy/đổi hiệu lực nguồn; không bịa giá để lấp.
            var next = calculated.get();
            if (same(row.calculation(), next.calculation())
                    && row.flags().equals(next.mergeFlags(row.flags()))
                    && rows.sameJson(row.unitPriceBasis(), next.unitPriceBasis())
                    && rows.sameJson(row.sourceValues(), next.sourceValues())) continue;
            rows.apply(row, next);
            var now = time.now();
            boolean reset = !row.status().equals("DRAFT");
            var details = new CostRecalculation(
                    row.calculation(),
                    next.calculation(),
                    causeKey,
                    reset,
                    row.unitPriceBasis(),
                    next.unitPriceBasis(),
                    row.sourceValues(),
                    next.sourceValues());
            history.add(
                    CostRecalculation.TRACK,
                    row.id(),
                    new HistoryEntry<>(
                            HistoryKind.POINT,
                            row.status(),
                            "DRAFT",
                            null,
                            now,
                            "RECALCULATE",
                            null,
                            true,
                            new Reason("Cập nhật căn cứ tính giá khoản chi phí"),
                            causeType,
                            causeId,
                            details,
                            null,
                            "M11.RECALCULATION:" + key));
            audit.write(AuditLogEntry.builder()
                    .action(CostRecalculation.PRICE_RECALCULATED)
                    .entity("M11.COST_ITEM", row.id())
                    .resident(row.residentId())
                    .before(Map.of(
                            "calculation",
                            row.calculation(),
                            "unitPriceBasis",
                            row.unitPriceBasis(),
                            "sourceValues",
                            row.sourceValues(),
                            "status",
                            row.status()))
                    .after(Map.of(
                            "calculation",
                            next.calculation(),
                            "unitPriceBasis",
                            next.unitPriceBasis(),
                            "sourceValues",
                            next.sourceValues(),
                            "status",
                            "DRAFT"))
                    .basis(new Basis("BR-M11-02", null, causeId, null))
                    .details(Map.of("eventKey", causeKey, "basisType", causeType))
                    .build());
            events.publishEvent(new CostItemChanged(
                    row.residentId(), row.periodId(), CostPricingNotifications.forChange(row, next, key, now)));
        }
    }

    private boolean same(CostRecalculation.Calculation first, CostRecalculation.Calculation second) {
        return decimal(first.quantity(), second.quantity())
                && decimal(first.unitPrice(), second.unitPrice())
                && decimal(first.factorPercent(), second.factorPercent())
                && decimal(first.amount(), second.amount());
    }

    private boolean decimal(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }
}
