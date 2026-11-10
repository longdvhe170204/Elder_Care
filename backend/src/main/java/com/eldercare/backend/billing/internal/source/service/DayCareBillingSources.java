package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.source.domain.BillingSourceKey;
import com.eldercare.backend.billing.internal.source.domain.CostSourceCandidate;
import com.eldercare.backend.billing.internal.source.domain.IndexedSourceMatch;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Buổi bán trú (FR-005b, FR-009): buổi có lịch tính từ 004 lúc tra cứu; buổi phát sinh ngoài lịch lấy từ chỉ mục (sự kiện
 * điểm danh của 005). Mã nguồn {@code <người>:<ngày>:<thứ tự buổi>} hoặc {@code <người>:<ngày>:AD_HOC}; giá theo dịch vụ
 * "buổi bán trú" của hợp đồng, giá tháng chia đều số buổi có lịch của cả tháng (trừ ngày khu nghỉ).
 */
@Service
public class DayCareBillingSources {
    public static final String UNIT = "SESSION";

    private final ResidencyDayApi days;
    private final ContractReadApi contracts;
    private final DayCareCalendarApi calendar;
    private final UnitPriceApi prices;
    private final BillingSourceIndexStore index;

    public DayCareBillingSources(
            ResidencyDayApi days,
            ContractReadApi contracts,
            DayCareCalendarApi calendar,
            UnitPriceApi prices,
            BillingSourceIndexStore index) {
        this.days = days;
        this.contracts = contracts;
        this.calendar = calendar;
        this.prices = prices;
        this.index = index;
    }

    public static String sourceId(long resident, LocalDate date, long sessionNo) {
        return resident + ":" + date + ":" + sessionNo;
    }

    public List<CostSourceCandidate> between(long resident, LocalDate from, LocalDate to) {
        var sessions = days.dayCareSessions(resident, from, to);
        var adHoc = index.find(resident, "DAY_CARE_SESSION", from, to);
        var ids = new ArrayList<String>();
        sessions.forEach(s -> ids.add(sourceId(resident, s.date(), s.sessionId())));
        adHoc.forEach(m -> ids.add(m.source().sourceId()));
        var existing = index.effectiveItems(resident, "DAY_CARE_SESSION", ids);
        var months = new HashMap<YearMonth, List<LocalDate>>();
        var result = new ArrayList<CostSourceCandidate>();
        for (var s : sessions) {
            String id = sourceId(resident, s.date(), s.sessionId());
            result.add(scheduled(resident, s, id, existing.get(new BillingSourceKey(id, UNIT)), months));
        }
        for (var m : adHoc) result.add(adHoc(m));
        result.sort((a, b) -> a.date().equals(b.date())
                ? a.sourceId().compareTo(b.sourceId())
                : a.date().compareTo(b.date()));
        return result;
    }

    private CostSourceCandidate scheduled(
            long resident, DayCareSessionView s, String id, Long existingItem, Map<YearMonth, List<LocalDate>> months) {
        var facts = new LinkedHashMap<String, Object>();
        facts.put("session", s);
        var contract = contracts.contentAtDate(resident, s.date());
        var service = contract.flatMap(this::dayCareService);
        String code = null;
        String reason = null;
        if (s.closureDay()) {
            code = "NOT_A_BILLABLE_EVENT";
            reason = "Ngày khu bán trú nghỉ";
        } else if (s.coefficientPercent() == null) {
            code = "CONDITION_NOT_MET";
            reason = "Buổi chưa xác định trạng thái";
        } else if (service.isEmpty()) {
            code = "CONDITION_NOT_MET";
            reason = "Hợp đồng chưa đăng ký dịch vụ buổi bán trú";
        }
        contract.ifPresent(c -> {
            facts.put("contractId", c.contractId());
            facts.put("contentVersion", c.version());
            String period = c.content().path("residencyFeeBillingPeriod").asString();
            facts.put("billingPeriod", period);
            if ("MONTH".equals(period)) {
                var scheduled = months.computeIfAbsent(YearMonth.from(s.date()), m -> scheduledDays(resident, m));
                facts.put("scheduledSessionCount", scheduled.size());
                facts.put(
                        "lastScheduledSession",
                        !scheduled.isEmpty() && scheduled.getLast().equals(s.date()) && lastOfDay(resident, s));
            }
        });
        return new CostSourceCandidate(
                resident,
                "DAY_CARE_SESSION",
                id,
                UNIT,
                s.date(),
                s.date().atStartOfDay(BusinessTime.ZONE).toInstant(),
                service.orElse(null),
                CostType.DAY_CARE_SESSION_FEE,
                "buổi",
                "Phí buổi bán trú",
                BigDecimal.ONE,
                s.coefficientPercent() == null ? null : BigDecimal.valueOf(s.coefficientPercent()),
                Set.of(),
                existingItem,
                code,
                reason,
                facts);
    }

    /** FR-005b (a): buổi phát sinh hệ số 100%, giá buổi của hợp đồng hoặc phiên bản đơn giá "buổi bán trú". */
    private CostSourceCandidate adHoc(IndexedSourceMatch m) {
        var s = m.source();
        var facts = new LinkedHashMap<String, Object>();
        facts.put("adHocSession", true);
        facts.put("billingPeriod", "SESSION");
        Long service = s.serviceId();
        if (service == null) {
            var contract = contracts.contentAtDate(s.residentId(), s.incurredOn());
            service = contract.flatMap(this::dayCareService)
                    .or(() -> {
                        var active = prices.activeServicesOf(ServiceType.DAY_CARE_SESSION);
                        return active.size() == 1
                                ? Optional.of(active.getFirst().id())
                                : Optional.empty();
                    })
                    .orElse(null);
        }
        return new CostSourceCandidate(
                s.residentId(),
                s.sourceType(),
                s.sourceId(),
                s.unit(),
                s.incurredOn(),
                s.occurredAt(),
                service,
                CostType.DAY_CARE_SESSION_FEE,
                "buổi",
                "Phí buổi bán trú phát sinh",
                BigDecimal.ONE,
                new BigDecimal("100"),
                Set.of(),
                m.existingItemId(),
                m.cancelled() ? "NOT_A_BILLABLE_EVENT" : null,
                m.cancelled() ? "Nguồn đã hủy" : null,
                facts);
    }

    private Optional<Long> dayCareService(ContractContentView c) {
        for (var item : c.content().path("services")) {
            long id = item.path("serviceId").asLong();
            var service = prices.service(id);
            if (service.isPresent() && service.get().type() == ServiceType.DAY_CARE_SESSION) return Optional.of(id);
        }
        return Optional.empty();
    }

    /** Ngày có buổi theo lịch đến của hợp đồng trong tháng, bỏ ngày khu nghỉ; mỗi buổi một phần tử. */
    private List<LocalDate> scheduledDays(long resident, YearMonth month) {
        var result = new ArrayList<LocalDate>();
        for (var d = month.atDay(1); !d.isAfter(month.atEndOfMonth()); d = d.plusDays(1)) {
            int lines = contracts.attendanceOn(resident, d).size();
            if (lines == 0 || calendar.onDate(resident, d).closureDay()) continue;
            for (int i = 0; i < lines; i++) result.add(d);
        }
        return result;
    }

    private boolean lastOfDay(long resident, DayCareSessionView s) {
        return s.sessionId() == contracts.attendanceOn(resident, s.date()).size();
    }
}
