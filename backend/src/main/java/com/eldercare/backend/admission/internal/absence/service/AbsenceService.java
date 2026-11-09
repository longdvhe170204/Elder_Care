package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.admission.api.events.AbsenceExtended;
import com.eldercare.backend.admission.api.events.ResidencyDaysChanged;
import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.domain.ExpectedReturnChange;
import com.eldercare.backend.admission.internal.absence.persistence.AbsenceStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lượt vắng (004 US6; FR-048 → FR-061, BR-M02-06, 07, 18; research T-08; Q-395): đọc, gia hạn dự kiến trở lại, Giải phóng giường,
 * đính chính. Nội trú vắng tính như ngày lưu trú, giữ giường không hạn; không có hệ số, không có quyết định giữ giường. Mở và đóng
 * lượt do {@link AbsenceTransition} làm trong lệnh trạng thái của 001. Mọi lệnh chạy dưới khóa theo người.
 */
@Service
public class AbsenceService {

    public static final String RECORD_TYPE = "M02.ABSENCE";
    static final String ENTITY_TYPE = "M02.ABSENCE";

    static final AuditAction EXTENDED = new AuditAction("M02_ABSENCE_EXTENDED", true);
    static final AuditAction BED_RELEASED = new AuditAction("M02_ABSENCE_BED_RELEASED", true);
    static final AuditAction RECALCULATED = new AuditAction("M02_ABSENCE_RECALCULATED", false);

    private final AbsenceStore store;
    private final ResidencyLock residencyLock;
    private final ObjectProvider<CorrectionApi> corrections;
    private final ObjectProvider<BedAllocationCommandApi> beds;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    AbsenceService(
            AbsenceStore store,
            ResidencyLock residencyLock,
            ObjectProvider<CorrectionApi> corrections,
            ObjectProvider<BedAllocationCommandApi> beds,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.corrections = corrections;
        this.beds = beds;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    /** Lượt vắng với giá trị hiện hành (đã áp đính chính) và lịch sử dự kiến trở lại. */
    public record Detail(Absence absence, List<ExpectedReturnChange> expectedReturnHistory) {}

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

    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Absence> ofResident(Target target) {
        return store.ofResident(target.residentId()).stream()
                .map(this::effective)
                .toList();
    }

    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Detail get(Target target, long id) {
        Absence absence = store.find(id).orElseThrow(NotFoundException::new);
        return new Detail(effective(absence), store.expectedReturnHistory(id));
    }

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
     * Giải phóng giường (FR-055, BR-M02-07, Q-345, Q-395): chỉ Quản lý viện, lượt đang mở, bắt buộc lý do. Trong cùng lệnh 003 đóng
     * phân bổ, giường chuyển Chờ sẵn sàng; từ ngày giải phóng, ngày vắng không tính phí lưu trú (FR-052) và 010 được báo. Module
     * phòng giường chưa có bản cài lệnh thì trả 503, không ghi gì.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public Detail releaseBed(Target target, long id, Reason reason) {
        CurrentUser user = currentUser.getObject();
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện giải phóng giường khi vắng (BR-M02-07, Q-345)");
        }
        Absence found = store.find(id).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        Absence absence = store.find(id).orElseThrow(NotFoundException::new);
        if (!absence.open()) {
            throw conflict("ABSENCE_CLOSED", "Lượt vắng đã đóng, không giải phóng giường được (FR-055)");
        }
        if (absence.bedReleasedAt() != null) {
            throw conflict("BED_ALREADY_RELEASED", "Giường của lượt vắng này đã được giải phóng");
        }
        Instant now = time.now();
        beds.getObject().releaseWhenAbsent(absence.residentId(), now, new Basis("BR-M02-07", null, id, null));
        store.markBedReleased(id, now, user.accountId(), reason.value());
        LocalDate day = now.atZone(BusinessTime.ZONE).toLocalDate();
        auditLog.write(AuditLogEntry.builder()
                .action(BED_RELEASED)
                .entity(ENTITY_TYPE, id)
                .resident(absence.residentId())
                .before(Map.of("holdBed", true))
                .after(Map.of("holdBed", false, "bedReleasedAt", now.toString()))
                .reason(reason)
                .build());
        events.publishEvent(new ResidencyDaysChanged(absence.residentId(), day, day, "BED_RELEASED"));
        return get(target, id);
    }

    /** Giá trị hiện hành của lượt vắng: bản gốc cộng đính chính mới nhất của thời điểm rời, về (FR-061). */
    public Absence effective(Absence absence) {
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

    /** Sau khi một đính chính lượt vắng được lưu (FR-061, BR-M02-18): báo 010 tính lại các ngày bị ảnh hưởng. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void afterCorrection(long absenceId, boolean voided) {
        Absence absence = store.find(absenceId).orElseThrow(NotFoundException::new);
        residencyLock.lock(absence.residentId());
        Absence current = voided ? absence : effective(absence);
        if (current.returnedAt() != null && current.returnedAt().isBefore(current.departedAt())) {
            throw new InvalidValueException("Thời điểm về phải sau thời điểm rời viện");
        }
        LocalDate from = earlier(dateOf(absence.departedAt()), dateOf(current.departedAt()));
        LocalDate to = later(lastDay(absence), lastDay(current));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("cause", "ABSENCE_CORRECTION");
        details.put("fromDate", from.toString());
        details.put("toDate", to.toString());
        auditLog.write(AuditLogEntry.builder()
                .action(RECALCULATED)
                .entity(ENTITY_TYPE, absence.id())
                .resident(absence.residentId())
                .details(details)
                .build());
        events.publishEvent(new ResidencyDaysChanged(absence.residentId(), from, to, "ABSENCE_CORRECTION"));
    }

    private LocalDate lastDay(Absence absence) {
        return absence.returnedAt() == null ? time.today() : dateOf(absence.returnedAt());
    }

    static LocalDate dateOf(Instant instant) {
        return instant.atZone(BusinessTime.ZONE).toLocalDate();
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant instantOf(Object value, Instant fallback) {
        return value == null ? fallback : Instant.parse(value.toString());
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value == null ? null : value.toString());
        return map;
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
