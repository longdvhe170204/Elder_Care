package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.billing.internal.pricing.domain.DayCareSessionFee;
import com.eldercare.backend.billing.internal.pricing.domain.ResidencyDayFee;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.BasisKind;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.PriceBasis;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.PriceQuote;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T014: giá của từng dịch vụ đã xác định; không tự cộng phí phòng/chăm sóc hoặc tự lập khoản. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CalendarCostPricing {
    private final ContractReadApi contracts;
    private final DayCareCalendarApi calendar;
    private final CostPricingService eventPricing;

    public CalendarCostPricing(
            ContractReadApi contracts, DayCareCalendarApi calendar, CostPricingService eventPricing) {
        this.contracts = contracts;
        this.calendar = calendar;
        this.eventPricing = eventPricing;
    }

    public record Slot(LocalDate date, long number) {}

    /** Dùng lại trong cùng transaction của lô; không cache toàn cục qua thay đổi hợp đồng/lịch. */
    public record MonthSchedule(long residentId, YearMonth month, List<Slot> slots) {
        public MonthSchedule {
            slots = slots.stream()
                    .sorted(Comparator.comparing(Slot::date).thenComparingLong(Slot::number))
                    .toList();
            if (new HashSet<>(slots).size() != slots.size()
                    || slots.stream()
                            .anyMatch(slot -> !YearMonth.from(slot.date()).equals(month))) {
                throw new IllegalArgumentException("Lịch phải gồm các buổi khác nhau trong đúng tháng");
            }
        }
    }

    public record CalendarQuote(
            long serviceId,
            PriceQuote price,
            BigDecimal sourceUnitPrice,
            String billingPeriod,
            BigDecimal factorPercent,
            Integer scheduledSessionCount,
            boolean lastScheduledSession,
            Map<String, Object> sourceValues) {
        public CalendarQuote {
            sourceValues = Map.copyOf(sourceValues);
        }
    }

    public CalendarQuote residencyDay(long residentId, long serviceId, ResidencyDayView source) {
        if (source.bedReleased()) throw condition("FR-007", "Không tính phí lưu trú từ ngày giải phóng giường.");
        if (source.outsideBillingPeriod()) throw outsidePeriod();
        var contract = contract(residentId, source.date());
        if ("DAY_CARE".equals(contract.content().path("residenceType").asString())) {
            throw condition("FR-005a", "Ngày nguồn đã chuyển sang hợp đồng bán trú; cần đọc lại nguồn lưu trú.");
        }
        if (source.contractId() == null || source.contractId() != contract.contractId()) {
            throw condition("FR-008", "Căn cứ hợp đồng của ngày lưu trú đã thay đổi; cần đọc lại nguồn.");
        }
        var period = period(contract);
        if (!Set.of("MONTH", "DAY").contains(period))
            throw condition("FR-009", "Phí nội trú cần kỳ tính tháng hoặc ngày.");
        var original = contract.recordedUnitPrices().get(serviceId);
        var base = original == null
                ? null
                : period.equals("MONTH")
                        ? ResidencyDayFee.base(original, source.date())
                        : ResidencyDayFee.direct(original);
        var values = new LinkedHashMap<String, Object>();
        values.put("date", source.date().toString());
        values.put("bedReleased", source.bedReleased());
        values.put("notYetAdmitted", source.notYetAdmitted());
        values.put("contractOverdue", source.contractOverdue());
        values.put("daysInMonth", source.date().lengthOfMonth());
        if (source.presence() != null) values.put("presence", source.presence());
        if (source.absenceId() != null) values.put("absenceId", source.absenceId());
        if (source.contentVersionId() != null) values.put("contentVersionId", source.contentVersionId());
        return quote(contract, serviceId, original, base, period, new BigDecimal("100"), null, false, values);
    }

    public CalendarQuote dayCareScheduled(
            long residentId, long serviceId, DayCareSessionView source, MonthSchedule schedule) {
        if (schedule != null
                && (schedule.residentId() != residentId || !schedule.month().equals(YearMonth.from(source.date())))) {
            throw new IllegalArgumentException("Lịch buổi không thuộc đúng người và tháng");
        }
        if (source.closureDay()) throw condition("FR-005b", "Không tính phí buổi trùng ngày khu bán trú nghỉ.");
        if (source.attendance() == null || source.coefficientPercent() == null) {
            throw condition("FR-005b", "Buổi bán trú chưa có trạng thái/hệ số được nguồn xác định.");
        }
        var contract = contract(residentId, source.date());
        requireDayCare(contract);
        String period = period(contract);
        if (!Set.of("MONTH", "SESSION").contains(period))
            throw condition("FR-009", "Bán trú cần kỳ tính tháng hoặc buổi.");
        var original = contract.recordedUnitPrices().get(serviceId);
        Integer count = null;
        boolean last = false;
        BigDecimal base = original;
        if (period.equals("MONTH")) {
            if (schedule == null || !schedule.slots().contains(new Slot(source.date(), source.sessionId()))) {
                throw condition("FR-009", "Thiếu lịch đầy đủ của tháng hoặc buổi không còn trong lịch.");
            }
            count = schedule.slots().size();
            last = schedule.slots().getLast().equals(new Slot(source.date(), source.sessionId()));
            base = original == null ? null : DayCareSessionFee.base(original, count, last);
        }
        var values = new LinkedHashMap<String, Object>();
        values.put("date", source.date().toString());
        values.put("sessionId", source.sessionId());
        values.put("attendance", source.attendance().name());
        values.put("coefficientPercent", source.coefficientPercent());
        values.put("closureDay", false);
        values.put("adHocSession", false);
        if (count != null) {
            values.put("scheduledSessionCount", count);
            values.put("lastScheduledSession", last);
        }
        return quote(
                contract,
                serviceId,
                original,
                base,
                period,
                BigDecimal.valueOf(source.coefficientPercent()),
                count,
                last,
                values);
    }

    public CalendarQuote dayCareAdHoc(long residentId, long contractedServiceId, long adHocServiceId, LocalDate day) {
        var contract = contract(residentId, day);
        requireDayCare(contract);
        var period = period(contract);
        if (!Set.of("MONTH", "SESSION").contains(period))
            throw condition("FR-009", "Bán trú cần kỳ tính tháng hoặc buổi.");
        var signed = contract.recordedUnitPrices().get(contractedServiceId);
        var values = Map.<String, Object>of(
                "date",
                day.toString(),
                "adHocSession",
                true,
                "coefficientPercent",
                100,
                "contractedServiceId",
                contractedServiceId,
                "adHocServiceId",
                adHocServiceId);
        if (period.equals("SESSION") && (signed != null || included(contract, contractedServiceId))) {
            return quote(
                    contract,
                    contractedServiceId,
                    signed,
                    signed,
                    "SESSION",
                    new BigDecimal("100"),
                    null,
                    false,
                    values);
        }
        var price = eventPricing.catalogue(adHocServiceId, day, BigDecimal.ONE);
        return new CalendarQuote(
                adHocServiceId, price, price.unitPrice(), "SESSION", new BigDecimal("100"), null, false, values);
    }

    /** Lịch từ phiên bản nội dung tại từng ngày, kể cả lịch sử; không dùng API bị cắt theo mốc dừng để chia giá tháng. */
    public MonthSchedule monthSchedule(long residentId, YearMonth month) {
        var slots = new ArrayList<Slot>();
        for (var day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            var content = contracts.contentAtDate(residentId, day);
            if (content.isEmpty()
                    || !"DAY_CARE"
                            .equals(content.get()
                                    .content()
                                    .path("residenceType")
                                    .asString())) continue;
            var arrivals = new ArrayList<LocalTime>();
            for (var line : content.get().content().path("arrivalSchedule")) {
                boolean weekly = line.hasNonNull("dayOfWeek")
                        && line.path("dayOfWeek").asInt() == day.getDayOfWeek().getValue();
                boolean dated = line.hasNonNull("date")
                        && LocalDate.parse(line.path("date").asString()).equals(day);
                if (weekly || dated)
                    arrivals.add(LocalTime.parse(line.path("arrivalTime").asString()));
            }
            if (arrivals.isEmpty()) continue;
            var calendarDay = calendar.onDate(residentId, day);
            if (calendarDay == null || !day.equals(calendarDay.date())) {
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SOURCE_UNAVAILABLE",
                        "Không đọc được lịch nghỉ bán trú đúng ngày.");
            }
            if (calendarDay.closureDay()) continue;
            arrivals.sort(Comparator.naturalOrder());
            for (int index = 0; index < arrivals.size(); index++) slots.add(new Slot(day, index + 1L));
        }
        return new MonthSchedule(residentId, month, slots);
    }

    private CalendarQuote quote(
            ContractContentView contract,
            long serviceId,
            BigDecimal original,
            BigDecimal base,
            String period,
            BigDecimal factor,
            Integer count,
            boolean last,
            Map<String, Object> values) {
        boolean inPackage = included(contract, serviceId);
        BigDecimal amount = inPackage ? BigDecimal.ZERO : base == null ? null : DayCareSessionFee.direct(base, factor);
        var basis = new PriceBasis(
                BasisKind.CONTRACT, contract.contractId(), contract.version(), contract.addendumId(), null);
        return new CalendarQuote(
                serviceId,
                new PriceQuote(base, amount, inPackage, basis),
                original,
                period,
                factor,
                count,
                last,
                values);
    }

    private boolean included(ContractContentView contract, long serviceId) {
        for (var service : contract.content().path("services")) {
            if (service.path("serviceId").asLong() == serviceId)
                return service.path("inPackage").asBoolean(false);
        }
        return false;
    }

    private ContractContentView contract(long residentId, LocalDate day) {
        return contracts.contentAtDate(residentId, day).orElseThrow(CalendarCostPricing::outsidePeriod);
    }

    private String period(ContractContentView contract) {
        return contract.content().path("residencyFeeBillingPeriod").asString();
    }

    private void requireDayCare(ContractContentView contract) {
        if (!"DAY_CARE".equals(contract.content().path("residenceType").asString())) {
            throw condition("FR-005b", "Ngày nguồn không thuộc nội dung hợp đồng bán trú.");
        }
    }

    private static BusinessException outsidePeriod() {
        return new BusinessException(
                HttpStatus.CONFLICT, "OUTSIDE_BILLING_PERIOD", "Không có hợp đồng hiệu lực tại ngày phát sinh.");
    }

    private static ConditionNotMetException condition(String code, String text) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, text)));
    }
}
