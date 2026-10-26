package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.events.AbsenceExtended;
import com.cambae.backend.m02.api.events.BedHoldDecisionNeeded;
import com.cambae.backend.m02.api.events.NoAbsencePolicyLine;
import com.cambae.backend.m02.api.events.ResidencyDaysChanged;
import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.AbsenceDay;
import com.cambae.backend.m02.internal.absence.domain.BedHoldChoice;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.domain.ExpectedReturnChange;
import com.cambae.backend.m02.internal.absence.domain.ThresholdDay;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m02.internal.absence.service.AbsenceDayCalculator.Written;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lượt vắng (004 US6; FR-048 → FR-061, BR-M02-06, 07, 17, 18; research T-08): đọc, gia hạn dự kiến trở lại, Quyết định giữ
 * giường và tính lại ngày vắng sau quyết định, đính chính. Mở và đóng lượt do {@link AbsenceTransition} làm trong lệnh
 * trạng thái của 001. Mọi lệnh chạy dưới khóa {@code resident_residency} của người cao tuổi.
 */
@Service
public class AbsenceService {

    public static final String RECORD_TYPE = "M02.ABSENCE";
    public static final String DECISION_RECORD_TYPE = "M02.BED_HOLD_DECISION";
    static final String ENTITY_TYPE = "M02.ABSENCE";

    static final AuditAction EXTENDED = new AuditAction("M02_ABSENCE_EXTENDED", true);
    static final AuditAction DECIDED = new AuditAction("M02_ABSENCE_DECISION", true);
    static final AuditAction RECALCULATED = new AuditAction("M02_ABSENCE_RECALCULATED", false);

    private final AbsenceStore store;
    private final AbsenceDayCalculator calculator;
    private final ResidencyLock residencyLock;
    private final ObjectProvider<CorrectionApi> corrections;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    AbsenceService(
            AbsenceStore store,
            AbsenceDayCalculator calculator,
            ResidencyLock residencyLock,
            ObjectProvider<CorrectionApi> corrections,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.calculator = calculator;
        this.residencyLock = residencyLock;
        this.corrections = corrections;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    /** Lượt vắng với giá trị hiện hành (đã áp đính chính), ngày vắng, quyết định và lịch sử dự kiến trở lại. */
    public record Detail(
            Absence absence,
            boolean holdBed,
            List<AbsenceDay> days,
            List<BedHoldDecision> decisions,
            List<ExpectedReturnChange> expectedReturnHistory) {}

    public static Target target(long absenceId, long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(absenceId), residentId, null);
    }

    public static Target targetForResident(long residentId) {
        return new Target(ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một lượt vắng; không có thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long absenceId) {
        Absence absence = store.find(absenceId).orElseThrow(NotFoundException::new);
        return target(absence.id(), absence.residentId());
    }

    /** Lượt Tạm vắng đang mở cho SPI của 001 (FR-050); lượt Bệnh viện không có mốc dự kiến nên không tính. */
    @Transactional(readOnly = true)
    public Optional<Absence> openTemporary(long residentId) {
        return store.open(residentId).filter(absence -> absence.type().isTemporaryAbsence());
    }

    // ---- Đọc ------------------------------------------------------------------------------------------------------

    /** Lượt vắng với tình trạng giường (đang giữ hay đã giải phóng). */
    public record Summary(Absence absence, boolean holdBed) {}

    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Summary> ofResident(Target target) {
        return store.ofResident(target.residentId()).stream()
                .map(absence -> new Summary(effective(absence), held(absence, effectiveDecisions(absence.id()))))
                .toList();
    }

    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Detail get(Target target, long id) {
        Absence absence = store.find(id).orElseThrow(NotFoundException::new);
        List<BedHoldDecision> decisions = effectiveDecisions(id);
        return new Detail(
                effective(absence),
                held(absence, decisions),
                store.days(id),
                decisions,
                store.expectedReturnHistory(id));
    }

    // ---- Lệnh -----------------------------------------------------------------------------------------------------

    /** Gia hạn dự kiến trở lại (FR-049): Hành chính, Trưởng tầng; lý do bắt buộc, lịch sử các mốc được giữ. */
    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public void extendExpectedReturn(Target target, long id, Instant expectedReturnAt, Reason reason) {
        Absence found = store.find(id).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        Absence absence = store.find(id).orElseThrow(NotFoundException::new);
        if (!absence.open()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ABSENCE_CLOSED", "Lượt vắng đã đóng, không gia hạn được (FR-061)");
        }
        if (!expectedReturnAt.isAfter(absence.departedAt())) {
            throw new InvalidValueException("expectedReturnAt phải sau thời điểm rời viện");
        }
        CurrentUser user = currentUser.getObject();
        Instant now = time.now();
        store.addExpectedReturnChange(
                id,
                new ExpectedReturnChange(
                        absence.expectedReturnAt(), expectedReturnAt, reason.value(), user.accountId(), now));
        store.updateExpectedReturn(id, expectedReturnAt);
        auditLog.write(AuditLogEntry.builder()
                .action(EXTENDED)
                .entity(ENTITY_TYPE, id)
                .resident(absence.residentId())
                .before(mapOf("expectedReturnAt", absence.expectedReturnAt()))
                .after(mapOf("expectedReturnAt", expectedReturnAt))
                .reason(reason)
                .build());
        events.publishEvent(new AbsenceExtended(id, absence.residentId(), expectedReturnAt));
    }

    /**
     * Quyết định giữ giường (FR-055, FR-056, FR-061): chỉ Quản lý viện. Lượt đang mở: Giữ tiếp (hệ số, ngày xem xét lại) hoặc
     * Giải phóng (hệ số các ngày còn lại, mặc định 0%); lượt đã đóng: chỉ đặt hệ số các ngày "chờ quyết định". Các ngày bị
     * ảnh hưởng được tính lại và 010 được báo. Việc giải phóng giường ở 003 chờ C công bố (plan 004, Phase 8).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public Detail decideBedHold(
            Target target, long id, BedHoldChoice choice, BigDecimal coefficient, LocalDate reviewDate, Reason reason) {
        CurrentUser user = currentUser.getObject();
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện ghi quyết định giữ giường (BR-M02-07, Q-345)");
        }
        Absence found = store.find(id).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        Absence absence = store.find(id).orElseThrow(NotFoundException::new);
        Absence current = effective(absence);
        List<BedHoldDecision> decisions = effectiveDecisions(id);
        LocalDate today = time.today();
        BigDecimal value = coefficient;
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidValueException("coefficient phải từ 0 đến 100");
        }
        LocalDate thresholdDate;
        LocalDate appliesFrom;
        LocalDate appliesTo = null;
        if (choice == BedHoldChoice.COEFFICIENT_ONLY) {
            if (current.open()) {
                throw conflict("ABSENCE_STILL_OPEN", "Lượt vắng còn mở: chọn Giữ tiếp hoặc Giải phóng");
            }
            List<AbsenceDay> pending = store.days(id).stream()
                    .filter(day -> day.pendingDecision() && day.decisionId() == null)
                    .toList();
            if (pending.isEmpty()) {
                throw conflict("NO_PENDING_DAYS", "Không có ngày nào đang chờ quyết định (FR-061)");
            }
            thresholdDate = pending.getFirst().date();
            appliesFrom = thresholdDate;
            appliesTo = pending.getLast().date();
        } else {
            if (!current.open()) {
                throw conflict("ABSENCE_CLOSED", "Lượt vắng đã đóng: chỉ đặt được hệ số (COEFFICIENT_ONLY)");
            }
            if (!held(absence, decisions)) {
                throw conflict("BED_NOT_HELD", "Giường không còn được giữ cho lượt vắng này");
            }
            if (choice == BedHoldChoice.HOLD_CONTINUE && (reviewDate == null || !reviewDate.isAfter(today))) {
                throw new InvalidValueException("reviewDate là ngày sau hôm nay, bắt buộc khi Giữ tiếp (FR-055)");
            }
            publishDayEvents(current, calculator.fillMissing(current, today, decisions));
            ThresholdDay threshold = store.firstNonHoldDay(id)
                    .orElseThrow(() -> conflict("THRESHOLD_NOT_REACHED", "Lượt vắng chưa tới ngưỡng giữ giường"));
            thresholdDate = threshold.date();
            appliesFrom = decisions.isEmpty() ? threshold.date() : today;
        }

        BedHoldDecision draft = new BedHoldDecision(
                0,
                id,
                thresholdDate,
                choice,
                value,
                choice == BedHoldChoice.HOLD_CONTINUE ? reviewDate : null,
                appliesFrom,
                appliesTo,
                reason.value(),
                user.accountId(),
                time.now());
        long decisionId = store.addDecision(draft);
        if (choice != BedHoldChoice.COEFFICIENT_ONLY) {
            store.setReviewDate(id, draft.reviewDate());
        }
        List<BedHoldDecision> all = effectiveDecisions(id);

        Written written = calculator.recalculate(current, appliesFrom, today, all);
        LocalDate last = written.days().isEmpty()
                ? appliesFrom
                : written.days().getLast().date();
        auditLog.write(AuditLogEntry.builder()
                .action(DECIDED)
                .entity(ENTITY_TYPE, id)
                .resident(absence.residentId())
                .after(decisionMap(decisionId, choice, value, draft.reviewDate(), appliesFrom, appliesTo))
                .reason(reason)
                .build());
        auditRecalculation(absence, appliesFrom, last, "BED_HOLD_DECISION");
        events.publishEvent(new ResidencyDaysChanged(absence.residentId(), appliesFrom, last, "BED_HOLD_DECISION"));
        return get(target, id);
    }

    // ---- Dùng trong module -----------------------------------------------------------------------------------------

    /** Giá trị hiện hành của lượt vắng: bản gốc cộng đính chính mới nhất của thời điểm rời, về (FR-061). */
    Absence effective(Absence absence) {
        CurrentValue value = corrections
                .getObject()
                .current(RECORD_TYPE, List.of(absence.id()))
                .get(absence.id());
        if (value == null || !value.corrected() || !(value.content() instanceof Map<?, ?> content)) {
            return absence;
        }
        Instant departed = instantOf(content.get("departedAt"), absence.departedAt());
        Instant returned = instantOf(content.get("returnedAt"), absence.returnedAt());
        return absence.withTimes(departed, returned);
    }

    /** Quyết định với hệ số và ngày xem xét lại đã áp đính chính (nhóm 3, FR-061). */
    List<BedHoldDecision> effectiveDecisions(long absenceId) {
        List<BedHoldDecision> raw = store.decisions(absenceId);
        if (raw.isEmpty()) {
            return raw;
        }
        Map<Long, CurrentValue> current = corrections
                .getObject()
                .current(
                        DECISION_RECORD_TYPE,
                        raw.stream().map(BedHoldDecision::id).toList());
        List<BedHoldDecision> result = new ArrayList<>();
        for (BedHoldDecision decision : raw) {
            CurrentValue value = current.get(decision.id());
            if (value != null && value.voided()) {
                continue;
            }
            if (value != null && value.corrected() && value.content() instanceof Map<?, ?> content) {
                BigDecimal coefficient = content.get("coefficient") == null
                        ? decision.coefficient()
                        : new BigDecimal(content.get("coefficient").toString());
                LocalDate reviewDate = content.get("reviewDate") == null
                        ? decision.reviewDate()
                        : LocalDate.parse(content.get("reviewDate").toString());
                decision = new BedHoldDecision(
                        decision.id(),
                        decision.absenceId(),
                        decision.thresholdDate(),
                        decision.choice(),
                        coefficient,
                        reviewDate,
                        decision.appliesFrom(),
                        decision.appliesTo(),
                        decision.reason(),
                        decision.decidedBy(),
                        decision.decidedAt());
            }
            result.add(decision);
        }
        return result;
    }

    boolean held(Absence absence, List<BedHoldDecision> decisions) {
        return AbsenceDayCalculator.held(absence, decisions, time.today());
    }

    /**
     * Sau khi một đính chính lượt vắng được lưu (FR-061, BR-M02-18): tính lại các ngày từ chỗ thay đổi, báo 010; ngày mới rơi
     * vào dòng "Quản lý quyết định" thì báo Quản lý viện để đặt hệ số.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void recalculateAfterCorrection(long absenceId, boolean voided) {
        Absence absence = store.find(absenceId).orElseThrow(NotFoundException::new);
        residencyLock.lock(absence.residentId());
        List<AbsenceDay> stored = store.days(absenceId);
        if (voided) {
            store.deleteDaysAfter(absenceId, LocalDate.of(1900, 1, 1));
            if (!stored.isEmpty()) {
                events.publishEvent(new ResidencyDaysChanged(
                        absence.residentId(),
                        stored.getFirst().date(),
                        stored.getLast().date(),
                        "ABSENCE_CORRECTION"));
            }
            return;
        }
        Absence current = effective(absence);
        if (current.returnedAt() != null && current.returnedAt().isBefore(current.departedAt())) {
            throw new InvalidValueException("Thời điểm về phải sau thời điểm rời viện");
        }
        List<BedHoldDecision> decisions = effectiveDecisions(absenceId);
        LocalDate firstNew = AbsenceDayCalculator.dateOf(current.departedAt());
        LocalDate lastNew = calculator.lastDay(current, time.today());
        LocalDate from;
        if (stored.isEmpty() || !stored.getFirst().date().equals(firstNew)) {
            from = firstNew;
        } else {
            LocalDate lastStored = stored.getLast().date();
            from = lastNew.isAfter(lastStored) ? lastStored.plusDays(1) : lastNew.plusDays(1);
        }
        Written written = calculator.recalculate(current, from, time.today(), decisions);
        LocalDate changedFrom = stored.isEmpty()
                ? from
                : (from.isBefore(stored.getLast().date())
                        ? from
                        : stored.getLast().date());
        LocalDate changedTo = written.days().isEmpty()
                ? stored.isEmpty() ? from : stored.getLast().date()
                : written.days().getLast().date();
        publishDayEvents(absence, written);
        auditRecalculation(absence, changedFrom, changedTo, "ABSENCE_CORRECTION");
        events.publishEvent(new ResidencyDaysChanged(
                absence.residentId(),
                changedFrom.isAfter(changedTo) ? changedTo : changedFrom,
                changedTo,
                "ABSENCE_CORRECTION"));
    }

    /** Sau đính chính một quyết định giữ giường: tính lại từ ngày quyết định áp dụng (FR-061). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void recalculateAfterDecisionCorrection(long decisionId) {
        BedHoldDecision decision = store.decisionById(decisionId).orElseThrow(NotFoundException::new);
        Absence absence = store.find(decision.absenceId()).orElseThrow(NotFoundException::new);
        residencyLock.lock(absence.residentId());
        Absence current = effective(absence);
        List<BedHoldDecision> decisions = effectiveDecisions(absence.id());
        Written written = calculator.recalculate(current, decision.appliesFrom(), time.today(), decisions);
        LocalDate last = written.days().isEmpty()
                ? decision.appliesFrom()
                : written.days().getLast().date();
        auditRecalculation(absence, decision.appliesFrom(), last, "BED_HOLD_DECISION_CORRECTION");
        events.publishEvent(new ResidencyDaysChanged(
                absence.residentId(), decision.appliesFrom(), last, "BED_HOLD_DECISION_CORRECTION"));
    }

    /** Báo Quản lý viện về ngày chờ quyết định và ngày không có dòng chính sách vừa ghi (FR-053, FR-056, FR-061). */
    void publishDayEvents(Absence absence, Written written) {
        Instant now = time.now();
        for (LocalDate date : written.newNoLine()) {
            events.publishEvent(new NoAbsencePolicyLine(absence.id(), absence.residentId(), date, now));
        }
        if (!written.newPending().isEmpty() && !absence.open()) {
            AbsenceDay last = written.days().stream()
                    .filter(AbsenceDay::pendingDecision)
                    .reduce((a, b) -> b)
                    .orElseThrow();
            events.publishEvent(new BedHoldDecisionNeeded(
                    absence.id(), absence.residentId(), last.sequenceNo(), last.policyLine(), now));
        }
    }

    private void auditRecalculation(Absence absence, LocalDate from, LocalDate to, String cause) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("cause", cause);
        details.put("fromDate", from.toString());
        details.put("toDate", to.toString());
        auditLog.write(AuditLogEntry.builder()
                .action(RECALCULATED)
                .entity(ENTITY_TYPE, absence.id())
                .resident(absence.residentId())
                .details(details)
                .build());
    }

    private static Instant instantOf(Object value, Instant fallback) {
        return value == null ? fallback : Instant.parse(value.toString());
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value == null ? null : value.toString());
        return map;
    }

    private static Map<String, Object> decisionMap(
            long id, BedHoldChoice choice, BigDecimal coefficient, LocalDate reviewDate, LocalDate from, LocalDate to) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("decisionId", id);
        map.put("choice", choice.name());
        map.put("coefficient", coefficient.toPlainString());
        map.put("reviewDate", reviewDate == null ? null : reviewDate.toString());
        map.put("appliesFrom", from.toString());
        map.put("appliesTo", to == null ? null : to.toString());
        return map;
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
