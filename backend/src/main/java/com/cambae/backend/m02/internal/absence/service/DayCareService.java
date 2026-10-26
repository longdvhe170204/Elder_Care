package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ArrivalScheduleView;
import com.cambae.backend.m02.api.events.DayCareAbsenceNoticeRecorded;
import com.cambae.backend.m02.api.events.DayCareClosureDayChanged;
import com.cambae.backend.m02.internal.absence.domain.ClosureDay;
import com.cambae.backend.m02.internal.absence.persistence.DayCareStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo vắng và ngày nghỉ của bán trú (004 US6; FR-060, FR-060a; research T-08). Hành chính ghi báo vắng kèm thời điểm gia đình
 * báo, hệ thống tự xác định có báo trước đủ CFG-M02-06 hay không; Hành chính khai báo ngày khu bán trú nghỉ, ngày đã qua đánh
 * dấu khai báo muộn. Hệ số từng buổi do 010 tra qua trạng thái có mặt của 005; spec này chỉ cung cấp dữ liệu và sự kiện.
 */
@Service
public class DayCareService {

    static final String NOTICE_ENTITY = "M02.DAY_CARE_ABSENCE_NOTICE";
    static final String CLOSURE_ENTITY = "M02.DAY_CARE_CLOSURE_DAY";

    static final AuditAction NOTICE_RECORDED = new AuditAction("M02_ABSENCE_NOTICE_RECORDED", false);
    static final AuditAction CLOSURE_DECLARED = new AuditAction("M02_ABSENCE_CLOSURE_DECLARED", true);
    static final AuditAction CLOSURE_CANCELLED = new AuditAction("M02_ABSENCE_CLOSURE_CANCELLED", true);

    private final DayCareStore store;
    private final ContractReadApi contracts;
    private final ParameterApi parameters;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    DayCareService(
            DayCareStore store,
            ContractReadApi contracts,
            ParameterApi parameters,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.contracts = contracts;
        this.parameters = parameters;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    public static Target targetForResident(long residentId) {
        return new Target(NOTICE_ENTITY, null, residentId, null);
    }

    public static Target closureTarget(Long id) {
        return new Target(CLOSURE_ENTITY, id == null ? null : String.valueOf(id), null, null);
    }

    /**
     * Ghi nhận gia đình báo vắng bán trú (FR-060): trả có báo trước đủ CFG-M02-06 hay không. Buổi thuộc 005 nên 004 dùng giờ
     * đến sớm nhất của ngày theo lịch đến của hợp đồng.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public boolean recordAbsenceNotice(
            Target target, long residentId, LocalDate date, Long sessionId, Instant familyNotifiedAt) {
        requireOffice();
        Instant now = time.now();
        if (familyNotifiedAt.isAfter(now)) {
            throw new InvalidValueException("familyNotifiedAt không được ở tương lai");
        }
        residencyLock.lock(residentId);
        LocalTime arrivalTime = contracts.arrivalScheduleAtDate(residentId, date).stream()
                .map(ArrivalScheduleView::arrivalTime)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT,
                        "NO_ARRIVAL_ON_DATE",
                        "Người cao tuổi không có lịch đến bán trú vào ngày " + date));
        Instant arrival = date.atTime(arrivalTime).atZone(BusinessTime.ZONE).toInstant();
        Duration required = parameters.read(ParameterCode.CFG_M02_06).asDuration();
        boolean withinNotice =
                !Duration.between(familyNotifiedAt, arrival).minus(required).isNegative();
        long id = store.insertNotice(
                residentId,
                date,
                sessionId,
                familyNotifiedAt,
                withinNotice,
                currentUser.getObject().accountId(),
                now);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("date", date.toString());
        after.put("sessionId", sessionId);
        after.put("familyNotifiedAt", familyNotifiedAt.toString());
        after.put("withinNoticePeriod", withinNotice);
        auditLog.write(AuditLogEntry.builder()
                .action(NOTICE_RECORDED)
                .entity(NOTICE_ENTITY, id)
                .resident(residentId)
                .after(after)
                .build());
        events.publishEvent(new DayCareAbsenceNoticeRecorded(residentId, date, sessionId, withinNotice));
        return withinNotice;
    }

    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ClosureDay> closures(Target target, LocalDate from, LocalDate to) {
        return store.closures(from, to);
    }

    /** Khai báo ngày khu bán trú nghỉ (FR-060a); ngày đã qua là khai báo muộn, 010 tính lại khoản, 011 không tính suất. */
    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public ClosureDay declareClosure(Target target, LocalDate date, Reason reason) {
        requireOffice();
        if (store.activeClosureExists(date)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "DAY_ALREADY_DECLARED", "Ngày " + date + " đã được khai báo nghỉ");
        }
        boolean late = date.isBefore(time.today());
        long id = store.insertClosure(
                date, reason.value(), late, currentUser.getObject().accountId(), time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(CLOSURE_DECLARED)
                .entity(CLOSURE_ENTITY, id)
                .after(Map.of("date", date.toString(), "lateDeclaration", late))
                .reason(reason)
                .build());
        events.publishEvent(new DayCareClosureDayChanged(date, true));
        return store.findClosure(id).orElseThrow();
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public ClosureDay cancelClosure(Target target, long id, Reason reason) {
        requireOffice();
        ClosureDay closure = store.findClosure(id).orElseThrow(NotFoundException::new);
        if (!closure.active()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "CLOSURE_ALREADY_CANCELLED", "Khai báo ngày nghỉ này đã được hủy");
        }
        store.cancelClosure(id, reason.value(), currentUser.getObject().accountId(), time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(CLOSURE_CANCELLED)
                .entity(CLOSURE_ENTITY, id)
                .before(Map.of("date", closure.date().toString(), "status", "ACTIVE"))
                .after(Map.of("date", closure.date().toString(), "status", "CANCELLED"))
                .reason(reason)
                .build());
        events.publishEvent(new DayCareClosureDayChanged(closure.date(), false));
        return store.findClosure(id).orElseThrow();
    }

    private void requireOffice() {
        if (!currentUser.getObject().roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính ghi báo vắng và ngày nghỉ bán trú (FR-060)");
        }
    }
}
