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
    private final ResidencyDayApi days;
    private final ContractReadApi contracts;
    private final UnitPriceApi prices;
    private final BillingSourceIndexStore index;
    private final com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window;

    public CostSourceLookup(
            ResidencyBillingSources residential,
            ResidencyDayApi days,
            ContractReadApi contracts,
            UnitPriceApi prices,
            BillingSourceIndexStore index,
            com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window) {
        this.residential = residential;
        this.days = days;
        this.contracts = contracts;
        this.prices = prices;
        this.index = index;
        this.window = window;
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
        if ("DAY_CARE_SESSION".equals(type)) {
            var sessions = days.dayCareSessions(resident, from, to);
            var existing = index.effectiveItems(
                    resident,
                    type,
                    sessions.stream().map(s -> Long.toString(s.sessionId())).toList());
            return sessions.stream()
                    .map(s -> new CostSourceCandidate(
                            resident,
                            type,
                            Long.toString(s.sessionId()),
                            "SESSION",
                            s.date(),
                            s.date().atStartOfDay(BusinessTime.ZONE).toInstant(),
                            null,
                            CostType.DAY_CARE_SESSION_FEE,
                            "buổi",
                            "Phí bán trú",
                            BigDecimal.ONE,
                            s.coefficientPercent() == null ? null : BigDecimal.valueOf(s.coefficientPercent()),
                            Set.of(),
                            existing.get(new BillingSourceKey(Long.toString(s.sessionId()), "SESSION")),
                            "CONDITION_NOT_MET",
                            s.closureDay()
                                    ? "Ngày khu bán trú nghỉ"
                                    : s.coefficientPercent() == null
                                            ? "Buổi chưa xác định trạng thái"
                                            : "Nguồn chưa cung cấp định danh dịch vụ, lịch toàn tháng và dấu buổi phát sinh để tính phí",
                            Map.<String, Object>of("session", s)))
                    .toList();
        }
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

    public CostSourceCandidate indexed(IndexedSourceMatch match) {
        var s = match.source();
        // Chưa có metadata tài chính của B/C/D: giữ đúng lượng nhưng không suy đơn vị/loại từ khóa kỹ thuật.
        return new CostSourceCandidate(
                s.residentId(),
                s.sourceType(),
                s.sourceId(),
                s.unit(),
                s.incurredOn(),
                s.occurredAt(),
                s.serviceId(),
                null,
                null,
                "Bản ghi tính phí",
                s.quantity(),
                null,
                Set.of(),
                match.existingItemId(),
                window.afterDeath(s.residentId(), s.incurredOn(), s.occurredAt(), false)
                        ? "OUTSIDE_BILLING_PERIOD"
                        : match.cancelled() ? "NOT_A_BILLABLE_EVENT" : "CONDITION_NOT_MET",
                window.afterDeath(s.residentId(), s.incurredOn(), s.occurredAt(), false)
                        ? "Nguồn phát sinh sau thời điểm qua đời"
                        : match.cancelled()
                                ? "Nguồn đã hủy"
                                : "Nguồn chưa cung cấp đủ đơn vị sử dụng và loại dịch vụ/vật phẩm",
                Map.of("source", s, "sourceVersion", match.version(), "cancelled", match.cancelled()));
    }
}
