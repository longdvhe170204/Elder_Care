package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.Attendance;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.billing.internal.item.domain.CostPricingSnapshot;
import com.eldercare.backend.billing.internal.item.domain.RepriceableCost;
import com.eldercare.backend.common.error.BusinessException;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Tính lại giá trên snapshot nguồn đã nhận; sửa/hủy nguồn thuộc T021, không thay thế bằng nguồn rỗng. */
@Service
public class ExistingCostPricing {
    public record MonthKey(long residentId, YearMonth month) {}

    private final CostPricingService events;
    private final CalendarCostPricing calendar;
    private final ContractReadApi contracts;

    public ExistingCostPricing(CostPricingService events, CalendarCostPricing calendar, ContractReadApi contracts) {
        this.events = events;
        this.calendar = calendar;
        this.contracts = contracts;
    }

    public Optional<CostPricingSnapshot> calculate(
            RepriceableCost row, Map<MonthKey, CalendarCostPricing.MonthSchedule> schedules) {
        if ("CONTRACT".equals(row.unitPriceBasis().get("kind"))) {
            if (!(row.unitPriceBasis().get("contractId") instanceof Number expected)) throw missing("contractId");
            if (contracts
                    .contentAtDate(row.residentId(), row.incurredOn())
                    .filter(content -> content.contractId() == expected.longValue())
                    .isEmpty()) return Optional.empty();
        }
        if (row.sourceType().equals("RESIDENCY_DAY")) {
            if (Boolean.TRUE.equals(row.sourceValues().get("bedReleased"))
                    || Boolean.TRUE.equals(row.sourceValues().get("outsideBillingPeriod"))) return Optional.empty();
            var contractId = row.unitPriceBasis().get("contractId");
            if (!(contractId instanceof Number number)) throw missing("contractId");
            var source = new ResidencyDayView(
                    row.incurredOn(),
                    number.longValue(),
                    optionalNumber(row, "contentVersionId"),
                    (String) row.sourceValues().get("presence"),
                    optionalNumber(row, "absenceId"),
                    false,
                    Boolean.TRUE.equals(row.sourceValues().get("notYetAdmitted")),
                    Boolean.TRUE.equals(row.sourceValues().get("contractOverdue")),
                    false);
            return Optional.of(preserveSource(
                    row,
                    CostPricingSnapshots.calendar(calendar.residencyDay(row.residentId(), row.serviceId(), source))));
        }
        if (row.sourceType().equals("DAY_CARE_SESSION")) {
            if (Boolean.TRUE.equals(row.sourceValues().get("adHocSession"))) {
                long contracted = number(row, "contractedServiceId");
                long adHoc = number(row, "adHocServiceId");
                return Optional.of(preserveSource(
                        row,
                        CostPricingSnapshots.calendar(
                                calendar.dayCareAdHoc(row.residentId(), contracted, adHoc, row.incurredOn()))));
            }
            long sessionId = number(row, "sessionId");
            if (Boolean.TRUE.equals(row.sourceValues().get("closureDay"))) return Optional.empty();
            if (!(row.sourceValues().get("attendance") instanceof String attendance)
                    || row.calculation().factorPercent() == null) throw missing("attendance/coefficientPercent");
            var session = new DayCareSessionView(
                    row.incurredOn(),
                    sessionId,
                    Attendance.valueOf(attendance),
                    row.calculation().factorPercent().intValueExact(),
                    false);
            var key = new MonthKey(row.residentId(), YearMonth.from(row.incurredOn()));
            var schedule =
                    schedules.computeIfAbsent(key, value -> calendar.monthSchedule(value.residentId(), value.month()));
            return Optional.of(preserveSource(
                    row,
                    CostPricingSnapshots.calendar(
                            calendar.dayCareScheduled(row.residentId(), row.serviceId(), session, schedule))));
        }
        return Optional.of(CostPricingSnapshots.event(
                row.serviceId(),
                row.calculation().quantity(),
                events.event(
                        row.residentId(),
                        row.serviceId(),
                        row.incurredOn(),
                        row.calculation().quantity()),
                row.sourceValues()));
    }

    private long number(RepriceableCost row, String field) {
        if (row.sourceValues().get(field) instanceof Number number) return number.longValue();
        throw missing(field);
    }

    private CostPricingSnapshot preserveSource(RepriceableCost row, CostPricingSnapshot next) {
        var values = new LinkedHashMap<>(row.sourceValues());
        values.putAll(next.sourceValues());
        return new CostPricingSnapshot(next.calculation(), next.pricingFlags(), next.unitPriceBasis(), values);
    }

    private Long optionalNumber(RepriceableCost row, String field) {
        return row.sourceValues().get(field) instanceof Number number ? number.longValue() : null;
    }

    private BusinessException missing(String field) {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Khoản thiếu căn cứ nguồn để tính lại: " + field);
    }
}
