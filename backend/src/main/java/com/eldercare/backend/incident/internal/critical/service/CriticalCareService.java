package com.eldercare.backend.incident.internal.critical.service;

import static com.eldercare.backend.incident.internal.critical.domain.CriticalActions.CHOICE_EXECUTED;
import static com.eldercare.backend.incident.internal.critical.domain.CriticalActions.FLAG_CONFIRMED;
import static com.eldercare.backend.incident.internal.critical.domain.CriticalActions.FLAG_RECORDED;
import static com.eldercare.backend.incident.internal.critical.domain.CriticalActions.FLAG_REMOVED;
import static com.eldercare.backend.incident.internal.critical.domain.CriticalActions.WISH_RECONFIRMED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.events.CriticalFlagRecorded;
import com.eldercare.backend.incident.api.events.PalliativeCareChoiceRecorded;
import com.eldercare.backend.incident.api.events.TemporaryCriticalFlagConfirmationRequested;
import com.eldercare.backend.incident.api.events.TemporaryHomeLeaveRequested;
import com.eldercare.backend.incident.api.events.WishReconfirmationCompleted;
import com.eldercare.backend.incident.api.events.WishReconfirmationRequested;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.critical.domain.ChoiceExecutionContent;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagContent;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagStatus;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagType;
import com.eldercare.backend.incident.internal.critical.domain.ExecutionChoice;
import com.eldercare.backend.incident.internal.critical.domain.ReconfirmationResult;
import com.eldercare.backend.incident.internal.critical.domain.WishReconfirmationCommand;
import com.eldercare.backend.incident.internal.critical.domain.WishReconfirmationContent;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutCommand;
import com.eldercare.backend.incident.internal.emergency.domain.TransferRefusedException;
import com.eldercare.backend.incident.internal.emergency.service.TransferOutService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguy kịch (US11; FR-050a → FR-050g, BR-M05-15, 16, DBR-34; research S-13): ghi dấu chính thức hoặc dấu tạm, xác nhận,
 * gỡ, tự gỡ theo trạng thái; cảnh báo nguy kịch Khẩn cấp không gộp tạo cùng lệnh; kết quả xác nhận lại và lựa chọn
 * thực hiện là bản ghi nhóm 3 gắn cảnh báo. Quyền theo dòng "Dấu nguy kịch, xác nhận lại nguyện vọng" Phụ lục 27 (Bác
 * sĩ toàn viện; Điều dưỡng trong phạm vi, chú thích ³¹) cộng vai trò của từng lệnh (Q-218).
 */
@Service
public class CriticalCareService {
    public static final String ALERT_TYPE = "CRITICAL_CONDITION";
    static final String AUTO_REMOVAL_REASON = "Tự gỡ theo trạng thái";
    private static final Set<ResidentStatus> FLAGGABLE =
            Set.of(ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.OFF_SITE_ACTIVITY);
    private static final Set<AlertStatus> OPEN_ALERT =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS);
    private static final PermissionKey CREATE = new PermissionKey(Function.CRITICAL_SIGN, Operation.CREATE);
    private static final PermissionKey UPDATE = new PermissionKey(Function.CRITICAL_SIGN, Operation.UPDATE);

    private final CriticalCareStore store;
    private final AlertService alertService;
    private final AlertStore alerts;
    private final AlertResponsibility responsibility;
    private final MonitoringLock monitoringLock;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ResidentApi residents;
    private final EndOfLifeWishApi wishes;
    private final TransferOutService transfers;
    private final Optional<AlertStaffPort> staff;
    private final Optional<EmergencyFamilyPort> family;

    public CriticalCareService(
            CriticalCareStore store,
            AlertService alertService,
            AlertStore alerts,
            AlertResponsibility responsibility,
            MonitoringLock monitoringLock,
            PermissionApi permissions,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ResidentApi residents,
            EndOfLifeWishApi wishes,
            TransferOutService transfers,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) EmergencyFamilyPort family) {
        this.store = store;
        this.alertService = alertService;
        this.alerts = alerts;
        this.responsibility = responsibility;
        this.monitoringLock = monitoringLock;
        this.permissions = permissions;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.residents = residents;
        this.wishes = wishes;
        this.transfers = transfers;
        this.staff = Optional.ofNullable(staff);
        this.family = Optional.ofNullable(family);
    }

    public record Recorded(long flagId, long alertId) {}

    /**
     * Ghi dấu (Bác sĩ) hoặc dấu tạm (Điều dưỡng, chỉ khi không có Bác sĩ trực); cùng lệnh tạo cảnh báo Khẩn cấp khóa
     * {@code NGUY_KICH:<dauId>} (không gộp), gắn dấu vào cảnh báo và phát thông báo FR-050b.
     */
    @Transactional
    public Recorded record(long residentId, String typeValue, String assessment, CurrentUser user) {
        CriticalFlagType type = parse(CriticalFlagType.class, typeValue, "Loại dấu nguy kịch không hợp lệ");
        if (assessment == null || assessment.isBlank()) throw new InvalidValueException("Thiếu nhận định");
        var now = time.now();
        Long floorAreaId = responsibility.floorOf(residentId, now).orElse(null);
        require(user, CREATE, new Target(CriticalCareStore.FLAG_TYPE, null, residentId, floorAreaId));
        Role role = type == CriticalFlagType.OFFICIAL ? Role.DOCTOR : Role.NURSING_STAFF;
        if (!user.roles().contains(role)) {
            throw new PermissionDeniedException(
                    type == CriticalFlagType.OFFICIAL
                            ? "Chỉ Bác sĩ ghi dấu nguy kịch"
                            : "Chỉ Điều dưỡng ghi dấu nguy kịch tạm");
        }
        if (!FLAGGABLE.contains(residents.status(residentId))) {
            throw blocked("FR-050a", "Chỉ ghi dấu cho người Đang lưu trú, Tạm vắng hoặc Hoạt động bên ngoài");
        }
        monitoringLock.lock(residentId);
        if (store.openFlag(residentId).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CRITICAL_FLAG_EXISTS", "Đã có dấu nguy kịch đang mở");
        }
        boolean noOnDutyDoctor =
                staff.map(port -> port.onDutyDoctors(now)).orElse(List.of()).isEmpty();
        if (type == CriticalFlagType.PROVISIONAL && !noOnDutyDoctor) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ON_DUTY_DOCTOR_AVAILABLE", "Đang có Bác sĩ trực, Bác sĩ ghi dấu nguy kịch");
        }
        var status = type == CriticalFlagType.OFFICIAL ? CriticalFlagStatus.OPEN : CriticalFlagStatus.PROVISIONAL;
        var content = new CriticalFlagContent(
                type, assessment.strip(), user.accountId(), now, null, null, null, null, null, null);
        var flag = store.create(residentId, status, content, user.accountId());
        var alert = alertService
                .requestAlert(new CreateAlertCommand(
                        residentId,
                        floorAreaId,
                        ALERT_TYPE,
                        TypeLock.forCritical(flag.id()),
                        IncidentLevel.CRITICAL,
                        now,
                        CriticalCareStore.FLAG_TYPE,
                        flag.id(),
                        "Nguy kịch – thực hiện nguyện vọng cuối đời",
                        user.accountId()))
                .orElseThrow(() -> blocked("FR-050a", "Người cao tuổi đã ở trạng thái cuối"));
        alerts.linkCriticalFlag(alert.id(), flag.id());
        store.change(flag, status, content.withAlert(alert.id()));
        write(FLAG_RECORDED, flag.id(), residentId, null, Map.of("status", status.name(), "alertId", alert.id()), null);

        long actor = user.accountId();
        events.publishEvent(new CriticalFlagRecorded(
                flag.id(),
                alert.id(),
                residentId,
                type.name(),
                CriticalCareNotices.key(alert.id(), "CRITICAL_FLAG_RECORDED"),
                now,
                CriticalCareNotices.flagRecorded(alert.id(), residentId, floorAreaId, noOnDutyDoctor, actor, now)));
        List<Long> callOrder = family.map(port -> port.representatives(residentId)).orElse(List.of()).stream()
                .map(EmergencyFamilyContactView::familyMemberId)
                .toList();
        events.publishEvent(new WishReconfirmationRequested(
                alert.id(),
                residentId,
                CriticalCareNotices.key(alert.id(), "WISH_RECONFIRMATION_REQUESTED"),
                now,
                CriticalCareNotices.reconfirmationRequested(
                        alert.id(), residentId, floorAreaId, noOnDutyDoctor, callOrder, actor, now)));
        if (type == CriticalFlagType.PROVISIONAL) {
            events.publishEvent(new TemporaryCriticalFlagConfirmationRequested(
                    flag.id(),
                    CriticalCareNotices.key(alert.id(), "PROVISIONAL_CRITICAL_FLAG:" + flag.id()),
                    now,
                    CriticalCareNotices.provisionalConfirmation(
                            flag.id(), alert.id(), residentId, floorAreaId, actor, now)));
        }
        return new Recorded(flag.id(), alert.id());
    }

    /** Tạm → Đang mở: Bác sĩ xác nhận, ghi người xác nhận, thời điểm (FR-050a, Q-218). */
    @Transactional
    public void confirm(long flagId, CurrentUser user) {
        var flag = lockForDoctor(flagId, user);
        if (!CriticalFlagStatus.PROVISIONAL.name().equals(flag.status())) {
            throw blocked("FR-050a", "Chỉ dấu tạm được xác nhận");
        }
        var now = time.now();
        store.change(flag, CriticalFlagStatus.OPEN, flag.content().confirmed(user.accountId(), now));
        write(FLAG_CONFIRMED, flagId, flag.residentId(), flag.status(), Map.of("status", "OPEN"), null);
    }

    /** Đang mở, Tạm → Đã gỡ: Bác sĩ, bắt buộc lý do; cảnh báo nguy kịch vẫn phải đóng theo FR-050d. */
    @Transactional
    public void remove(long flagId, Reason reason, CurrentUser user) {
        if (reason == null) throw new ReasonRequiredException();
        var flag = lockForDoctor(flagId, user);
        if (CriticalFlagStatus.REMOVED.name().equals(flag.status())) {
            throw blocked("FR-050a", "Dấu nguy kịch đã gỡ");
        }
        store.change(
                flag, CriticalFlagStatus.REMOVED, flag.content().removed(user.accountId(), time.now(), reason.value()));
        write(FLAG_REMOVED, flagId, flag.residentId(), flag.status(), Map.of("status", "REMOVED"), reason);
    }

    /** Người cao tuổi chuyển Điều trị tại bệnh viện hoặc trạng thái cuối: Hệ thống gỡ dấu đang mở (bảng FR-050a). */
    @Transactional
    public void removeBySystem(long residentId, Instant at) {
        monitoringLock.lock(residentId);
        store.openFlag(residentId).ifPresent(open -> {
            var flag = store.lock(open.id());
            store.change(flag, CriticalFlagStatus.REMOVED, flag.content().removed(null, at, AUTO_REMOVAL_REASON));
            write(
                    FLAG_REMOVED,
                    flag.id(),
                    residentId,
                    flag.status(),
                    Map.of("status", "REMOVED"),
                    new Reason(AUTO_REMOVAL_REASON));
        });
    }

    /**
     * Kết quả xác nhận lại (FR-050c): Bác sĩ hoặc Điều dưỡng trong phạm vi; "nguyện vọng mới" ghi phiên bản qua 001
     * trong cùng lệnh (001 từ chối thì cả lệnh rollback). Kết quả đầu tiên phát {@link WishReconfirmationCompleted} để
     * 009 dừng nhắc gọi (FR-050b d).
     */
    @Transactional
    public long reconfirm(long alertId, WishReconfirmationCommand command, CurrentUser user) {
        var alert = lockCriticalAlert(alertId, user);
        if (command.channel() == null || command.result() == null || command.contactedAt() == null) {
            throw new InvalidValueException("Thiếu kênh, thời điểm hoặc kết quả xác nhận lại");
        }
        if (command.contactedFamilyMemberId() == null
                && (command.contactedName() == null || command.contactedName().isBlank())) {
            throw new InvalidValueException("Thiếu người được liên hệ");
        }
        Long newWishVersionId = null;
        if (command.result() == ReconfirmationResult.CHANGED) {
            if (command.newWish() == null) throw new InvalidValueException("Nguyện vọng mới là bắt buộc");
            newWishVersionId = wishes.recordVersion(
                    command.newWish().apply(alert.residentId()), WishRecordSource.CRITICAL_RECONFIRMATION);
        }
        boolean first = store.reconfirmations(alertId).isEmpty();
        var now = time.now();
        var row = store.addReconfirmation(
                alertId,
                new WishReconfirmationContent(
                        command.contactedFamilyMemberId(),
                        command.contactedName(),
                        command.channel(),
                        command.contactedAt(),
                        command.result(),
                        newWishVersionId),
                user.accountId(),
                now,
                alert.floorAreaId());
        var after = new HashMap<String, Object>();
        after.put("alertId", alertId);
        after.put("result", command.result().name());
        if (newWishVersionId != null) after.put("newWishVersionId", newWishVersionId);
        write(WISH_RECONFIRMED, CriticalCareStore.RECONFIRMATION_TYPE, row.id(), alert.residentId(), null, after, null);
        if (first) events.publishEvent(new WishReconfirmationCompleted(alertId));
        return row.id();
    }

    /**
     * Lựa chọn thực hiện (FR-050d) sau khi có kết quả xác nhận lại. Kết quả gần nhất "không liên lạc được": có nguyện
     * vọng Hiệu lực thì làm theo nguyện vọng đó; chưa có thì Bác sĩ quyết định kèm lý do (BR-M05-16).
     */
    @Transactional
    public long executeChoice(
            long alertId, String choiceValue, Reason reason, TransferOutCommand transferOut, CurrentUser user) {
        ExecutionChoice choice = parse(ExecutionChoice.class, choiceValue, "Lựa chọn thực hiện không hợp lệ");
        var alert = lockCriticalAlert(alertId, user);
        var results = store.reconfirmations(alertId);
        if (results.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "WISH_NOT_RECONFIRMED", "Chưa có kết quả xác nhận lại nguyện vọng");
        }
        if (!store.choices(alertId).isEmpty()) {
            throw blocked("FR-050g", "Lựa chọn đã ghi; sai sót xử lý bằng đính chính");
        }
        if (results.getLast().content().result() == ReconfirmationResult.UNREACHABLE) {
            var wish = wishes.current(alert.residentId());
            if (wish.isPresent()) {
                if (!wish.get().choice().name().equals(choice.name())) {
                    throw blocked("BR-M05-16", "Không liên lạc được: thực hiện theo nguyện vọng Hiệu lực");
                }
            } else {
                if (reason == null) throw new ReasonRequiredException();
                if (!user.roles().contains(Role.DOCTOR)) {
                    throw new PermissionDeniedException(
                            "Chưa có nguyện vọng Hiệu lực: Bác sĩ quyết định theo chuyên môn");
                }
            }
        }
        if (choice == ExecutionChoice.TRANSFER_TO_HOSPITAL) {
            // Lệnh Chuyển viện của US5 chạy trên sự cố đã liên kết với cảnh báo (chuyển sự cố hoặc kích hoạt khẩn
            // cấp); 001 từ chối thì cả lệnh rollback, không ghi lựa chọn (FR-049, "hoặc toàn bộ, hoặc không").
            if (transferOut == null) throw new InvalidValueException("Thiếu thông tin Chuyển viện");
            if (alert.incidentId() == null) {
                throw blocked("FR-049", "Chuyển cảnh báo thành sự cố hoặc kích hoạt khẩn cấp trước khi chuyển viện");
            }
            try {
                transfers.transfer(alert.incidentId(), transferOut, user);
            } catch (TransferRefusedException refused) {
                throw refused.refusal();
            }
        }
        var now = time.now();
        var row = store.addChoice(
                alertId,
                new ChoiceExecutionContent(choice, reason == null ? null : reason.value(), null),
                user.accountId(),
                now,
                alert.floorAreaId());
        write(
                CHOICE_EXECUTED,
                CriticalCareStore.CHOICE_TYPE,
                row.id(),
                alert.residentId(),
                null,
                Map.of("alertId", alertId, "choice", choice.name()),
                reason);
        long residentId = alert.residentId();
        switch (choice) {
            case RETURN_HOME ->
                events.publishEvent(new TemporaryHomeLeaveRequested(
                        residentId,
                        alertId,
                        CriticalCareNotices.key(alertId, "TEMPORARY_HOME_LEAVE_REQUESTED"),
                        now,
                        CriticalCareNotices.homeLeave(
                                alertId, residentId, alert.floorAreaId(), user.accountId(), now)));
            case STAY_PALLIATIVE ->
                events.publishEvent(new PalliativeCareChoiceRecorded(
                        residentId, CriticalCareNotices.key(alertId, "PALLIATIVE_CARE_CHOICE"), now));
            case TRANSFER_TO_HOSPITAL -> {}
        }
        return row.id();
    }

    private SimpleRecord<CriticalFlagContent> lockForDoctor(long flagId, CurrentUser user) {
        var flag = store.lock(flagId);
        var floorAreaId = flag.content().alertId() == null
                ? null
                : alerts.findById(flag.content().alertId())
                        .map(AlertRecord::floorAreaId)
                        .orElse(null);
        require(
                user,
                UPDATE,
                new Target(CriticalCareStore.FLAG_TYPE, Long.toString(flagId), flag.residentId(), floorAreaId));
        if (!user.roles().contains(Role.DOCTOR)) throw new PermissionDeniedException("Chỉ Bác sĩ xác nhận hoặc gỡ dấu");
        return flag;
    }

    private AlertRecord lockCriticalAlert(long alertId, CurrentUser user) {
        var alert = alerts.lock(alertId).orElseThrow(NotFoundException::new);
        if (alert.criticalFlagId() == null) throw blocked("FR-050c", "Cảnh báo không phải cảnh báo nguy kịch");
        require(
                user,
                UPDATE,
                new Target(
                        CriticalCareNotices.SOURCE_TYPE,
                        Long.toString(alertId),
                        alert.residentId(),
                        alert.floorAreaId()));
        if (!user.roles().contains(Role.DOCTOR) && !user.roles().contains(Role.NURSING_STAFF)) {
            throw new PermissionDeniedException("Chỉ Bác sĩ hoặc Điều dưỡng ghi kết quả xác nhận lại và lựa chọn");
        }
        if (!OPEN_ALERT.contains(alert.status())) throw blocked("FR-050e", "Cảnh báo nguy kịch đã kết thúc");
        return alert;
    }

    private void require(CurrentUser user, PermissionKey key, Target target) {
        var result = permissions.hasPermission(user, key, target);
        if (result.allowed()) return;
        if (result.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
        throw new PermissionDeniedException("Không có quyền với dấu nguy kịch của người này");
    }

    private void write(
            AuditAction action, long flagId, Long residentId, String before, Map<String, Object> after, Reason reason) {
        write(action, CriticalCareStore.FLAG_TYPE, flagId, residentId, before, after, reason);
    }

    private void write(
            AuditAction action,
            String entity,
            long id,
            Long residentId,
            String beforeStatus,
            Map<String, Object> after,
            Reason reason) {
        var entry = AuditLogEntry.builder().action(action).entity(entity, id).after(after);
        if (residentId != null) entry.resident(residentId);
        if (beforeStatus != null) entry.before(Map.of("status", beforeStatus));
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String message) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException(message);
        }
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
