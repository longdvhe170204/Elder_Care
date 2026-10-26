package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.api.dto.PickupRecordView;
import com.cambae.backend.m02.api.events.AbsenceClosed;
import com.cambae.backend.m02.api.events.AbsenceOpened;
import com.cambae.backend.m02.api.events.BedHoldDecisionNeeded;
import com.cambae.backend.m02.api.spi.PickupRecordPort;
import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore.NewAbsence;
import com.cambae.backend.m02.internal.absence.service.AbsenceDayCalculator.Written;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Hold;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Outcome;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Tác động của lệnh trạng thái 001 lên lượt vắng (FR-048, FR-054, FR-057, FR-059; research T-08): Cho tạm vắng mở lượt, Chuyển
 * viện mở hoặc nối lượt Bệnh viện, Ghi nhận trở về đóng lượt. Chạy trong transaction của lệnh (MANDATORY), dưới khóa {@code
 * resident_residency}. Giữ và giải phóng giường ở 003 chờ C công bố nên chưa gọi (plan 004, Phase 8).
 */
@Component
public class AbsenceTransition {

    static final AuditAction OPENED = new AuditAction("M02_ABSENCE_OPENED", false);
    static final AuditAction CLOSED = new AuditAction("M02_ABSENCE_CLOSED", false);

    private final AbsenceStore store;
    private final AbsenceDayCalculator calculator;
    private final AbsenceService service;
    private final ObjectProvider<PickupRecordPort> pickups;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    AbsenceTransition(
            AbsenceStore store,
            AbsenceDayCalculator calculator,
            AbsenceService service,
            ObjectProvider<PickupRecordPort> pickups,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.calculator = calculator;
        this.service = service;
        this.pickups = pickups;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    /**
     * Điều kiện của Cho tạm vắng: người đón có bản ghi đón hợp lệ của 012 (FR-048). Chưa có bản cài thật của 012 thì chưa
     * thỏa, giống điều kiện người đại diện của hợp đồng.
     */
    public List<Condition> grantConditions(CommandContext context) {
        JsonNode details = context.details();
        long pickerId = details.get("pickedUpById").asLong();
        long recordId = details.get("pickupRecordId").asLong();
        PickupRecordPort port = pickups.getIfAvailable();
        Optional<PickupRecordView> record =
                port == null ? Optional.empty() : port.validPickupRecord(context.residentId(), pickerId, context.at());
        if (record.isPresent() && record.get().id() == recordId) {
            return List.of();
        }
        return List.of(new Condition(
                "ABS_PICKUP_RECORD", "Người đón chưa có bản ghi đón hợp lệ (feature 012)", "FR-048", false));
    }

    /** Cho tạm vắng: mở lượt, dùng bản ghi đón, tra dòng chính sách ngày 1 để biết giữ giường (FR-048, FR-054, BR-M02-17). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void granted(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        JsonNode d = result.details();
        AbsenceType type = AbsenceType.valueOf(d.get("absenceType").asString());
        Instant expected = Instant.parse(d.get("expectedReturnAt").asString());
        if (!expected.isAfter(result.at())) {
            throw new InvalidValueException("details.expectedReturnAt phải sau thời điểm rời viện");
        }
        requireNoOpen(residentId);
        long pickupRecordId = d.get("pickupRecordId").asLong();
        pickups.getObject().markUsed(pickupRecordId, result.command().name(), result.historyId());
        Outcome dayOne = calculator.dayOne(residentId, type, AbsenceDayCalculator.dateOf(result.at()));
        long id = store.insert(new NewAbsence(
                residentId,
                type,
                null,
                text(d, "standardReason"),
                result.at(),
                expected,
                d.get("pickedUpById").asLong(),
                pickupRecordId,
                null,
                d.get("handoverStaffId").asLong(),
                d.has("takesMedication") && d.get("takesMedication").asBoolean(),
                dayOne.holdBed(),
                null,
                currentUser.getObject().accountId(),
                result.at()));
        opened(store.find(id).orElseThrow(), dayOne);
    }

    /**
     * Chuyển viện (FR-057): đang Tạm vắng thì đóng lượt tại thời điểm chuyển và mở lượt Bệnh viện từ cùng thời điểm, đếm lại từ
     * ngày 1, giường đang giữ thì giữ liên tục; ngày chuyển thuộc lượt mới.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void transferredOut(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        JsonNode d = result.details();
        Optional<Absence> previous = store.open(residentId);
        boolean hold;
        if (previous.isPresent()) {
            hold = service.held(
                    previous.get(), service.effectiveDecisions(previous.get().id()));
            store.close(
                    previous.get().id(), result.at(), currentUser.getObject().accountId());
        } else {
            hold = calculator
                    .dayOne(residentId, AbsenceType.HOSPITAL, AbsenceDayCalculator.dateOf(result.at()))
                    .holdBed();
        }
        Instant expected =
                d.has("expectedReturnAt") && !d.get("expectedReturnAt").isNull()
                        ? Instant.parse(d.get("expectedReturnAt").asString())
                        : null;
        long id = store.insert(new NewAbsence(
                residentId,
                AbsenceType.HOSPITAL,
                null,
                null,
                result.at(),
                expected,
                null,
                null,
                d.get("transferUnit").asString(),
                d.get("handoverStaffId").asLong(),
                false,
                hold,
                previous.map(Absence::id).orElse(null),
                currentUser.getObject().accountId(),
                result.at()));
        if (previous.isPresent()) {
            Absence closed = store.find(previous.get().id()).orElseThrow();
            Written written = calculator.fillMissing(
                    closed, AbsenceDayCalculator.dateOf(result.at()), service.effectiveDecisions(closed.id()));
            service.publishDayEvents(closed, written);
            audit(CLOSED, closed, Map.of("returnedAt", result.at().toString(), "cause", "TRANSFER_OUT"));
            events.publishEvent(new AbsenceClosed(closed.id(), residentId, result.at()));
        }
        opened(
                store.find(id).orElseThrow(),
                calculator.dayOne(residentId, AbsenceType.HOSPITAL, AbsenceDayCalculator.dateOf(result.at())));
    }

    /** Ghi nhận trở về (FR-059): ghi nốt các ngày tới ngày về rồi đóng lượt; không có lượt mở thì không làm gì. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void returned(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        Optional<Absence> open = store.open(residentId);
        if (open.isEmpty()) {
            return;
        }
        Absence absence = open.get();
        JsonNode d = result.details();
        Instant returnedAt = d.has("returnedAt") && !d.get("returnedAt").isNull()
                ? Instant.parse(d.get("returnedAt").asString())
                : result.at();
        if (returnedAt.isBefore(absence.departedAt()) || returnedAt.isAfter(result.at())) {
            throw new InvalidValueException("details.returnedAt phải từ lúc rời viện tới hiện tại");
        }
        List<BedHoldDecision> decisions = service.effectiveDecisions(absence.id());
        Absence closing = absence.withTimes(absence.departedAt(), returnedAt);
        Written written = calculator.fillMissing(closing, AbsenceDayCalculator.dateOf(returnedAt), decisions);
        store.close(absence.id(), returnedAt, currentUser.getObject().accountId());
        service.publishDayEvents(closing, written);
        audit(CLOSED, absence, Map.of("returnedAt", returnedAt.toString(), "cause", "RECORD_RETURN"));
        events.publishEvent(new AbsenceClosed(absence.id(), residentId, returnedAt));
    }

    /** Giường đã giải phóng thì báo cần phân bổ giường mới khi người đó trở về (FR-059). */
    public List<Warning> returnWarnings(long residentId) {
        return store.ofResident(residentId).stream()
                .filter(a -> !a.open())
                .findFirst()
                .filter(closed ->
                        !AbsenceDayCalculator.held(closed, service.effectiveDecisions(closed.id()), time.today()))
                .map(closed -> List.of(new Warning(
                        "BED_REALLOCATION_NEEDED", "Giường đã được giải phóng, cần phân bổ giường mới (FR-059)")))
                .orElse(List.of());
    }

    private void opened(Absence absence, Outcome dayOne) {
        Instant now = time.now();
        Written written = calculator.fillMissing(absence, AbsenceDayCalculator.dateOf(absence.departedAt()), List.of());
        audit(OPENED, absence, Map.of("type", absence.type().name(), "initialBedHold", absence.initialBedHold()));
        events.publishEvent(new AbsenceOpened(absence.id(), absence.residentId(), absence.departedAt()));
        service.publishDayEvents(absence, written);
        if (absence.initialBedHold() && dayOne.hold() == Hold.MANAGER_DECIDES) {
            store.markThresholdNotified(absence.id(), now);
            events.publishEvent(new BedHoldDecisionNeeded(absence.id(), absence.residentId(), 1, dayOne.line(), now));
        }
    }

    private void requireNoOpen(long residentId) {
        if (store.open(residentId).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ABSENCE_ALREADY_OPEN", "Người cao tuổi đã có một lượt vắng đang mở");
        }
    }

    private void audit(AuditAction action, Absence absence, Map<String, ?> details) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(AbsenceService.ENTITY_TYPE, absence.id())
                .resident(absence.residentId())
                .details(new LinkedHashMap<String, Object>(details))
                .build());
    }

    private static String text(JsonNode details, String field) {
        return details.has(field) && !details.get(field).isNull()
                ? details.get(field).asString()
                : null;
    }
}
