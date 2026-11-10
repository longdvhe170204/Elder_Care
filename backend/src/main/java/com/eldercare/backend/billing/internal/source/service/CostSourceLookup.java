package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.admission.api.*;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.source.domain.*;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** FR-005: nguồn có khoản vẫn được trả về; không đọc bảng riêng của module nguồn. */
@Service
public class CostSourceLookup {
    private final ResidencyBillingSources residential;
    private final ContractReadApi contracts;
    private final UnitPriceApi prices;
    private final BillingSourceIndexStore index;
    private final com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window;
    private final DayCareBillingSources dayCare;
    private final SourceServiceMetadata metadata;

    public CostSourceLookup(
            ResidencyBillingSources residential,
            ContractReadApi contracts,
            UnitPriceApi prices,
            BillingSourceIndexStore index,
            com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window,
            DayCareBillingSources dayCare,
            SourceServiceMetadata metadata) {
        this.residential = residential;
        this.contracts = contracts;
        this.prices = prices;
        this.index = index;
        this.window = window;
        this.dayCare = dayCare;
        this.metadata = metadata;
    }

    public List<CostSourceCandidate> find(long resident, String type, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw new InvalidValueException("Khoảng ngày không hợp lệ");
        if ("RESIDENCY_DAY".equals(type))
            return residential.between(resident, from, to).stream()
                    .map(s -> {
                        var original = s.day();
                        var d = original.outsideBillingPeriod() && window.overdueBillable(resident, original.date())
                                ? new com.eldercare.backend.admission.api.dto.ResidencyDayView(
                                        original.date(),
                                        original.contractId(),
                                        original.contentVersionId(),
                                        original.presence(),
                                        original.absenceId(),
                                        original.bedReleased(),
                                        original.notYetAdmitted(),
                                        original.contractOverdue(),
                                        false)
                                : original;
                        var contract = contracts.contentAtDate(resident, d.date());
                        var service = contract.flatMap(
                                c -> c.content().path("roomTypeId").isNumber()
                                        ? prices.residencyFeeServiceOf(
                                                c.content().path("roomTypeId").asLong())
                                        : Optional.empty());
                        var flags = new LinkedHashSet<String>();
                        if (d.notYetAdmitted()) flags.add("NOT_YET_MOVED_IN");
                        var facts = new LinkedHashMap<String, Object>();
                        facts.put("day", d);
                        contract.ifPresent(c -> {
                            facts.put(
                                    "billingPeriod",
                                    c.content()
                                            .path("residencyFeeBillingPeriod")
                                            .asString());
                            facts.put("contractId", c.contractId());
                            facts.put("contentVersion", c.version());
                            facts.put("recordedUnitPrices", c.recordedUnitPrices());
                        });
                        String code = window.afterDeath(resident, d.date(), null, true)
                                ? "OUTSIDE_BILLING_PERIOD"
                                : d.bedReleased()
                                        ? "NOT_A_BILLABLE_EVENT"
                                        : d.outsideBillingPeriod() ? "OUTSIDE_BILLING_PERIOD" : null;
                        String reason = d.bedReleased()
                                ? "Ngày từ khi giải phóng giường không tính phí"
                                : "Ngày ngoài thời gian tính phí";
                        if (code == null && service.isEmpty()) {
                            code = "CONDITION_NOT_MET";
                            reason = "Nguồn chưa cung cấp đủ dịch vụ tính phí lưu trú";
                        }
                        return new CostSourceCandidate(
                                resident,
                                type,
                                s.key().sourceId(),
                                "DAY",
                                d.date(),
                                d.date().atStartOfDay(BusinessTime.ZONE).toInstant(),
                                service.map(v -> v.id()).orElse(null),
                                CostType.RESIDENCY_FEE,
                                "ngày",
                                "Phí lưu trú",
                                BigDecimal.ONE,
                                new BigDecimal("100"),
                                flags,
                                s.existingItemId(),
                                code,
                                reason,
                                facts);
                    })
                    .toList();
        if ("DAY_CARE_SESSION".equals(type)) return dayCare.between(resident, from, to);
        if (!Set.of(
                        "CARE_TASK_RESULT",
                        "DOSE",
                        "PRN_ADMINISTRATION",
                        "DISPENSE_RECORD",
                        "FAMILY_MEAL",
                        "OVERNIGHT_STAY",
                        "ACTIVITY_ATTENDANCE")
                .contains(type)) throw new InvalidValueException("Loại nguồn không hợp lệ");
        return index.find(resident, type, from, to).stream().map(this::indexed).toList();
    }

    /** Q3: dịch vụ duy nhất Hiệu lực của loại nguồn không mang mã dịch vụ. */
    public Optional<Long> serviceForUnresolved(String sourceType) {
        return metadata.serviceForUnresolved(sourceType);
    }

    public CostSourceCandidate indexed(IndexedSourceMatch match) {
        var s = match.source();
        boolean afterDeath = window.afterDeath(s.residentId(), s.incurredOn(), s.occurredAt(), false);
        var meta = metadata.resolve(s);
        // FR-011: ngoài thời gian tính phí (trước ngày bắt đầu hợp đồng, sau mốc dừng của 004) bị chặn.
        boolean outside = !afterDeath && window.outsideEventPeriod(s.residentId(), s.incurredOn());
        String code = afterDeath || outside
                ? "OUTSIDE_BILLING_PERIOD"
                : match.cancelled()
                        ? "NOT_A_BILLABLE_EVENT"
                        : meta.blockedReason() != null ? "CONDITION_NOT_MET" : null;
        String reason = afterDeath
                ? "Nguồn phát sinh sau thời điểm qua đời"
                : outside
                        ? "Nguồn ngoài thời gian tính phí"
                        : match.cancelled() ? "Nguồn đã hủy" : meta.blockedReason();
        return new CostSourceCandidate(
                s.residentId(),
                s.sourceType(),
                s.sourceId(),
                s.unit(),
                s.incurredOn(),
                s.occurredAt(),
                meta.serviceId(),
                meta.costType(),
                meta.unit(),
                meta.description() == null ? "Bản ghi tính phí" : meta.description(),
                s.quantity(),
                null,
                Set.of(),
                match.existingItemId(),
                code,
                reason,
                Map.of("source", s, "sourceVersion", match.version(), "cancelled", match.cancelled()));
    }
}
