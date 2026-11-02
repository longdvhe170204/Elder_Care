package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.api.dto.PickupRecordView;
import com.eldercare.backend.admission.api.events.AbsenceClosed;
import com.eldercare.backend.admission.api.events.AbsenceOpened;
import com.eldercare.backend.admission.api.spi.PickupRecordPort;
import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.domain.AbsenceType;
import com.eldercare.backend.admission.internal.absence.domain.NewAbsence;
import com.eldercare.backend.admission.internal.absence.persistence.AbsenceStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.dto.Warning;
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
 * Tác động của lệnh trạng thái 001 lên lượt vắng (FR-048, FR-054, FR-057, FR-059; research T-08; Q-395): Cho tạm vắng mở lượt, Chuyển
 * viện mở hoặc nối lượt Bệnh viện, Ghi nhận trở về đóng lượt. Chạy trong transaction của lệnh (MANDATORY), dưới khóa theo người. Giữ
 * giường do bean của 003 làm trong cùng lệnh, hỏi {@code AllocationResidencySource.holdBedWhenAbsent} (luôn giữ, không hạn).
 */
@Component
public class AbsenceTransition {

    static final AuditAction OPENED = new AuditAction("M02_ABSENCE_OPENED", false);
    static final AuditAction CLOSED = new AuditAction("M02_ABSENCE_CLOSED", false);

    private final AbsenceStore store;
    private final ObjectProvider<PickupRecordPort> pickups;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<CurrentUser> currentUser;

    AbsenceTransition(
            AbsenceStore store,
            ObjectProvider<PickupRecordPort> pickups,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.pickups = pickups;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.events = events;
        this.currentUser = currentUser;
    }

    /**
     * Điều kiện của Cho tạm vắng: người đón có bản ghi đón hợp lệ của 012 (FR-048). Chưa có bản cài thật của 012 thì chưa
     * thỏa, giống điều kiện người đại diện của hợp đồng.
     */
    public List<Condition> grantConditions(CommandContext context) {
        JsonNode details = context.details();
        if (details == null || !details.hasNonNull("pickedUpById") || !details.hasNonNull("pickupRecordId")) {
            return List.of(new Condition(
                    "ABS_PICKUP_RECORD", "Người đón chưa có bản ghi đón hợp lệ (feature 012)", "FR-048", false));
        }
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

    /** Cho tạm vắng: mở lượt, dùng bản ghi đón (FR-048, FR-054). */
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
                null,
                currentUser.getObject().accountId(),
                result.at()));
        opened(store.find(id).orElseThrow());
    }

    /** Chuyển viện (FR-057): đang Tạm vắng thì đóng lượt tại thời điểm chuyển và mở lượt Bệnh viện từ cùng thời điểm. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void transferredOut(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        JsonNode d = result.details();
        Optional<Absence> previous = store.open(residentId);
        previous.ifPresent(open ->
                store.close(open.id(), result.at(), currentUser.getObject().accountId()));
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
                previous.map(Absence::id).orElse(null),
                currentUser.getObject().accountId(),
                result.at()));
        if (previous.isPresent()) {
            Absence closed = store.find(previous.get().id()).orElseThrow();
            audit(CLOSED, closed, Map.of("returnedAt", result.at().toString(), "cause", "TRANSFER_OUT"));
            events.publishEvent(new AbsenceClosed(closed.id(), residentId, result.at()));
        }
        opened(store.find(id).orElseThrow());
    }

    /** Ghi nhận trở về (FR-059): đóng lượt; không có lượt mở thì không làm gì. */
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
        Instant returnedAt =
                d != null && d.has("returnedAt") && !d.get("returnedAt").isNull()
                        ? Instant.parse(d.get("returnedAt").asString())
                        : result.at();
        if (returnedAt.isBefore(absence.departedAt()) || returnedAt.isAfter(result.at())) {
            throw new InvalidValueException("details.returnedAt phải từ lúc rời viện tới hiện tại");
        }
        store.close(absence.id(), returnedAt, currentUser.getObject().accountId());
        audit(CLOSED, absence, Map.of("returnedAt", returnedAt.toString(), "cause", "RECORD_RETURN"));
        events.publishEvent(new AbsenceClosed(absence.id(), residentId, returnedAt));
    }

    /**
     * Người cao tuổi sang trạng thái cuối khi lượt vắng còn mở (Kết thúc lưu trú FR-065, Ghi nhận qua đời FR-070): đóng lượt tại
     * {@code closeAt} (không sớm hơn lúc rời viện). Không có lượt mở thì không làm gì.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void closedByFinalState(long residentId, Instant closeAt, String cause) {
        residencyLock.lock(residentId);
        Optional<Absence> open = store.open(residentId);
        if (open.isEmpty()) {
            return;
        }
        Absence absence = open.get();
        Instant closedAt = closeAt.isBefore(absence.departedAt()) ? absence.departedAt() : closeAt;
        store.close(absence.id(), closedAt, currentUser.getObject().accountId());
        audit(CLOSED, absence, Map.of("returnedAt", closedAt.toString(), "cause", cause));
        events.publishEvent(new AbsenceClosed(absence.id(), residentId, closedAt));
    }

    /** Giường đã giải phóng thì báo cần phân bổ giường mới khi người đó trở về (FR-059). */
    public List<Warning> returnWarnings(long residentId) {
        return store.open(residentId)
                .filter(absence -> !absence.holdBed())
                .map(absence -> List.of(new Warning(
                        "BED_REALLOCATION_NEEDED", "Giường đã được giải phóng, cần phân bổ giường mới (FR-059)")))
                .orElse(List.of());
    }

    private void opened(Absence absence) {
        audit(OPENED, absence, Map.of("type", absence.type().name()));
        events.publishEvent(new AbsenceOpened(absence.id(), absence.residentId(), absence.departedAt()));
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
