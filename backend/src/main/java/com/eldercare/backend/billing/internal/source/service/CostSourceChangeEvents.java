package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.admission.api.events.*;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.billing.internal.item.service.CostRecalculationService;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.MedicationBillAdjustedEvent;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** T021: chỉ mục, marker và tính lại/biến động cùng transaction; lỗi giữ publication để thử lại. */
@Component
class CostSourceChangeEvents {
    private final BillingSourceIndexStore index;
    private final CostRecalculationService costs;
    private final com.eldercare.backend.billing.internal.period.service.CostLock locks;
    private final ReminderMarkers markers;
    private final BusinessTime time;

    CostSourceChangeEvents(
            BillingSourceIndexStore index,
            CostRecalculationService costs,
            com.eldercare.backend.billing.internal.period.service.CostLock locks,
            ReminderMarkers markers,
            BusinessTime time) {
        this.index = index;
        this.costs = costs;
        this.locks = locks;
        this.markers = markers;
        this.time = time;
    }

    @ApplicationModuleListener
    void changed(CostSourceChanged event) {
        apply(event);
    }

    private void apply(CostSourceChanged event) {
        if (event.eventKey() == null
                || event.eventKey().isBlank()
                || event.basis() == null
                || (!event.cancelled() && event.newQuantity().isEmpty())
                || event.newQuantity()
                        .filter(q -> q.signum() < 0 || q.scale() > 3 || q.precision() - q.scale() > 9)
                        .isPresent())
            throw new IllegalArgumentException("Sự kiện đổi nguồn thiếu căn cứ, khóa hoặc lượng hợp lệ");
        var sources = index.byKey(event.sourceType(), event.sourceId(), event.unit());
        if (sources.isEmpty()) throw new IllegalStateException("Chờ sự kiện gốc trước khi xử lý đính chính");
        for (var source : sources) {
            long resident = source.source().residentId();
            locks.resident(resident);
            String marker = source.source().processingMarker("CHANGE:" + event.eventKey());
            if (!markers.mark(
                    "M11.SOURCE_PROCESSED", "M11.BILLING_SOURCE", resident, marker, Map.of("basis", event.basis())))
                continue;
            index.change(
                    event.sourceType(),
                    event.sourceId(),
                    event.unit(),
                    event.newQuantity().orElse(null),
                    event.cancelled(),
                    time.now());
            costs.sourceChanged(
                    event.sourceType(),
                    event.sourceId(),
                    event.unit(),
                    event.newQuantity().orElse(null),
                    event.cancelled(),
                    event.basis(),
                    marker);
        }
    }

    @ApplicationModuleListener
    void care(BillableCareResultCorrected event) {
        boolean cancelled = "VOID".equals(event.correctionType());
        if (!cancelled && !"AMEND_CONTENT".equals(event.correctionType()))
            throw new IllegalArgumentException("Loại đính chính nguồn không hợp lệ");
        var c = event.context();
        apply(new CostSourceChanged(
                "CARE_TASK_RESULT",
                Long.toString(event.resultId()),
                "UNIT",
                Optional.ofNullable(event.newQuantity()),
                cancelled,
                c.sourceType() + ":" + c.sourceId(),
                c.sourceType() + ":" + c.sourceId() + ":" + c.milestone()));
    }

    /**
     * 006 đính chính liều/PRN hoặc nhận lại thuốc mang theo (FR-020). Lượng 0 là không còn nguồn tính phí. Sự kiện không có
     * khóa riêng nên áp theo trạng thái (lặp lại cho cùng kết quả); nguồn chưa từng vào chỉ mục (chưa phát sinh phí) thì
     * bỏ qua.
     */
    @ApplicationModuleListener
    void medication(MedicationBillAdjustedEvent event) {
        String type = event.source().name();
        String id = Long.toString(event.sourceId());
        boolean cancelled = event.newQuantity().signum() == 0;
        BigDecimal quantity = cancelled ? null : event.newQuantity();
        String basis = "M06." + event.reason().name();
        for (var match : index.byTypeAndId(type, id)) {
            var source = match.source();
            locks.resident(source.residentId());
            index.change(type, id, source.unit(), quantity, cancelled, time.now());
            costs.sourceChanged(type, id, source.unit(), quantity, cancelled, basis, null);
        }
    }

    /** 014 FR-031, FR-031a: cùng trường với {@link CostSourceChanged}. */
    @ApplicationModuleListener
    void activity(ActivityCostSourceChanged event) {
        apply(new CostSourceChanged(
                event.sourceType(),
                event.sourceId(),
                event.unit(),
                event.newQuantity(),
                event.cancelled(),
                event.basis(),
                event.eventKey()));
    }

    @ApplicationModuleListener
    void price(PriceVersionCreated event) {
        costs.refresh(
                null, event.serviceId(), event.effectiveFrom(), event.effectiveTo(), true, "PRICE_VERSION_CREATED");
    }

    @ApplicationModuleListener
    void addendum(AddendumApplied event) {
        costs.refresh(event.residentId(), null, event.effectiveDate(), null, false, "ADDENDUM:" + event.addendumId());
    }

    @ApplicationModuleListener
    void residency(ResidencyDaysChanged event) {
        costs.calendar(event.residentId(), event.fromDate(), event.toDate(), "RESIDENCY_DAY", event.cause());
    }

    @ApplicationModuleListener
    void attendance(AttendanceChanged event) {
        var month = YearMonth.from(event.date());
        costs.calendar(
                event.residentId(),
                month.atDay(1),
                month.atEndOfMonth(),
                "DAY_CARE_SESSION",
                "ATTENDANCE:" + event.context().sourceId() + ":"
                        + event.context().milestone());
    }

    @ApplicationModuleListener
    void closure(DayCareClosureDayChanged event) {
        var month = YearMonth.from(event.date());
        costs.calendar(
                null,
                month.atDay(1),
                month.atEndOfMonth(),
                "DAY_CARE_SESSION",
                "DAY_CARE_CLOSURE:" + event.date() + ":" + event.active());
    }

    @ApplicationModuleListener
    void absence(DayCareAbsenceNoticeRecorded event) {
        costs.calendar(
                event.residentId(), event.date(), event.date(), "DAY_CARE_SESSION", "DAY_CARE_ABSENCE:" + event.date());
    }
}
