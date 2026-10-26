package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.EndCase;
import com.cambae.backend.m02.api.events.ResidencyEndingCancelled;
import com.cambae.backend.m02.api.events.ResidencyEndingCreated;
import com.cambae.backend.m02.api.events.ResidencyEndingDateChanged;
import com.cambae.backend.m02.api.spi.FinalPeriodPort;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.Handover;
import com.cambae.backend.m02.internal.ending.domain.RecipientType;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore.NewEnding;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.CreateRequest;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hồ sơ kết thúc lưu trú (004 US7; FR-062 → FR-068, BR-M02-19; research T-09): lập (kèm bảng kỳ cuối nháp ở 010), đổi ngày
 * kết thúc dự kiến, hủy, ghi nhận bàn giao và lập yêu cầu ngoại lệ. Hoàn tất qua lệnh Kết thúc lưu trú của 001 ({@link
 * EndingTransition}). Mọi lệnh chạy dưới khóa {@code resident_residency} của người cao tuổi, rồi khóa dòng hồ sơ.
 */
@Service
public class ResidencyEndingService {

    public static final String ENTITY_TYPE = "M02.RESIDENCY_ENDING";

    static final AuditAction CREATED = new AuditAction("M02_ENDING_CREATED", true);
    static final AuditAction DATE_CHANGED = new AuditAction("M02_ENDING_DATE_CHANGED", true);
    static final AuditAction CANCELLED = new AuditAction("M02_ENDING_CANCELLED", true);
    static final AuditAction HANDOVER_RECORDED = new AuditAction("M02_ENDING_HANDOVER_RECORDED", false);

    private static final Set<ResidentStatus> CAN_END =
            Set.of(ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED);
    private static final Set<EndCase> MEDICAL_CASES = Set.of(EndCase.DISCHARGE, EndCase.TRANSFER_TO_ANOTHER_FACILITY);

    private final ResidencyEndingStore store;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final ObjectProvider<FinalPeriodPort> finalPeriods;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final EndingHandovers handovers;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    ResidencyEndingService(
            ResidencyEndingStore store,
            ResidencyLock residencyLock,
            ResidentApi residents,
            ObjectProvider<FinalPeriodPort> finalPeriods,
            ObjectProvider<ApprovalRequestApi> approvals,
            EndingHandovers handovers,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.finalPeriods = finalPeriods;
        this.approvals = approvals;
        this.handovers = handovers;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    /** Dữ liệu lập hồ sơ kết thúc. */
    public record NewEndingInput(
            long residentId,
            EndCase endCase,
            boolean medicalReason,
            LocalDate expectedEndDate,
            String destination,
            String requestedBy) {}

    /** Dữ liệu bàn giao người cao tuổi. */
    public record HandoverInput(
            RecipientType recipientType,
            Long familyMemberId,
            String facilityName,
            Instant handoverAt,
            long handoverStaffId) {}

    /** Hồ sơ kèm lần bàn giao hiện hành. */
    public record Detail(ResidencyEnding ending, Optional<EndingHandovers.Effective> handover) {}

    public static Target target(long endingId, long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(endingId), residentId, null);
    }

    public static Target targetForResident(long residentId) {
        return new Target(ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một hồ sơ; không có thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long endingId) {
        ResidencyEnding ending = store.find(endingId).orElseThrow(NotFoundException::new);
        return target(ending.id(), ending.residentId());
    }

    // ---- Đọc ------------------------------------------------------------------------------------------------------

    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Detail get(Target target, long id) {
        ResidencyEnding ending = store.find(id).orElseThrow(NotFoundException::new);
        return new Detail(ending, handovers.latest(id));
    }

    // ---- Lệnh -----------------------------------------------------------------------------------------------------

    /**
     * Lập hồ sơ kết thúc (FR-062, FR-066): Hành chính, hoặc Bác sĩ với Xuất viện, Chuyển cơ sở vì lý do y tế. Mỗi người tối đa
     * một hồ sơ Đang chuẩn bị. Trong cùng transaction tạo bảng kỳ cuối nháp ở 010 (tác động kèm lệnh, Q-306).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.CREATE)
    @Transactional
    public ResidencyEnding create(Target target, NewEndingInput input, Reason reason) {
        CurrentUser user = currentUser.getObject();
        requireCreateRole(user, input);
        if (input.endCase() == EndCase.DEATH) {
            throw new InvalidValueException("endCase không nhận DEATH: qua đời có luồng riêng (FR-070)");
        }
        if (input.endCase() == EndCase.TRANSFER_TO_ANOTHER_FACILITY
                && (input.destination() == null || input.destination().isBlank())) {
            throw new InvalidValueException("Chuyển cơ sở cần nơi chuyển đến (destination, FR-062)");
        }
        if (input.requestedBy() == null || input.requestedBy().isBlank()) {
            throw new InvalidValueException("requestedBy là bắt buộc (FR-062)");
        }
        long residentId = input.residentId();
        if (!CAN_END.contains(residents.status(residentId))) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENT_NOT_IN_RESIDENCE",
                    "Chỉ lập hồ sơ kết thúc cho người Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện (FR-062)");
        }
        FinalPeriodPort port = finalPeriods.getIfAvailable();
        if (port == null) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "FINAL_PERIOD_UNAVAILABLE",
                    "Chưa tạo được bảng kỳ cuối nháp (feature 010 chưa công bố giao diện); chưa lập được hồ sơ"
                            + " kết thúc (FR-066)")));
        }
        residencyLock.lock(residentId);
        if (store.lockPreparingOf(residentId).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENCY_ENDING_ALREADY_OPEN",
                    "Người cao tuổi đã có hồ sơ kết thúc lưu trú Đang chuẩn bị (FR-062)");
        }
        Instant now = time.now();
        long id = store.insert(new NewEnding(
                residentId,
                input.endCase(),
                input.medicalReason(),
                blankToNull(input.destination()),
                input.expectedEndDate(),
                input.requestedBy().strip(),
                reason.value(),
                user.accountId(),
                now));
        store.setFinalPeriodRef(id, port.createDraft(residentId, input.expectedEndDate()));
        ResidencyEnding created = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(Map.of(
                        "endCase", input.endCase().name(),
                        "expectedEndDate", input.expectedEndDate().toString(),
                        "medicalReason", input.medicalReason()))
                .reason(reason)
                .build());
        events.publishEvent(new ResidencyEndingCreated(id, residentId, input.expectedEndDate()));
        return created;
    }

    /**
     * Đổi ngày kết thúc dự kiến kèm lý do (FR-067): mốc dừng tính phí dời theo, kỳ cuối nháp tính lại. Dấu "quá ngày dự
     * kiến" gỡ khi ngày mới không sớm hơn hôm nay; ngày mới đã qua thì dấu tính từ ngày liền sau ngày mới (FR-066a).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.UPDATE)
    @Transactional
    public ResidencyEnding changeDate(Target target, long id, LocalDate newDate, Reason reason) {
        requireOfficeOrDoctor(currentUser.getObject());
        ResidencyEnding ending = lockedPreparing(id);
        LocalDate overdue = newDate.isBefore(time.today()) ? newDate.plusDays(1) : null;
        store.updateExpectedEndDate(id, newDate, overdue);
        finalPeriods.getObject().recalculate(ending.residentId(), newDate);
        auditLog.write(AuditLogEntry.builder()
                .action(DATE_CHANGED)
                .entity(ENTITY_TYPE, id)
                .resident(ending.residentId())
                .before(mapOf("expectedEndDate", ending.expectedEndDate(), "overdueSince", ending.overdueSince()))
                .after(mapOf("expectedEndDate", newDate, "overdueSince", overdue))
                .reason(reason)
                .build());
        events.publishEvent(new ResidencyEndingDateChanged(id, ending.residentId(), newDate));
        return store.find(id).orElseThrow();
    }

    /** Hủy hồ sơ kèm lý do (FR-066, FR-067): các ngày sau mốc đã dừng lại là sự kiện tính phí. */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.UPDATE)
    @Transactional
    public void cancel(Target target, long id, Reason reason) {
        CurrentUser user = currentUser.getObject();
        requireOfficeOrDoctor(user);
        ResidencyEnding ending = lockedPreparing(id);
        store.cancel(id, reason.value(), user.accountId(), time.now());
        finalPeriods.getObject().cancelStop(ending.residentId());
        auditLog.write(AuditLogEntry.builder()
                .action(CANCELLED)
                .entity(ENTITY_TYPE, id)
                .resident(ending.residentId())
                .before(Map.of("status", ending.status().name()))
                .after(Map.of("status", "CANCELLED"))
                .reason(reason)
                .build());
        events.publishEvent(new ResidencyEndingCancelled(id, ending.residentId(), ending.expectedEndDate()));
    }

    /**
     * Ghi nhận bàn giao người cao tuổi (FR-063 (e); nhóm 3): người nhận là người thân hoặc cơ sở tiếp nhận, thời điểm, nhân
     * viên bàn giao. Sai sót đi đường đính chính {@code M02.RESIDENT_HANDOVER}.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.CREATE)
    @Transactional
    public void recordHandover(Target target, long id, HandoverInput input) {
        CurrentUser user = currentUser.getObject();
        requireOfficeOrDoctor(user);
        validate(input);
        ResidencyEnding ending = lockedPreparing(id);
        long handoverId = store.insertHandover(new Handover(
                0,
                id,
                input.recipientType(),
                input.recipientType() == RecipientType.FAMILY_MEMBER ? input.familyMemberId() : null,
                input.recipientType() == RecipientType.RECEIVING_FACILITY
                        ? input.facilityName().strip()
                        : null,
                input.handoverAt(),
                input.handoverStaffId(),
                user.accountId(),
                time.now()));
        auditLog.write(AuditLogEntry.builder()
                .action(HANDOVER_RECORDED)
                .entity(ENTITY_TYPE, id)
                .resident(ending.residentId())
                .details(Map.of(
                        "handoverId",
                        handoverId,
                        "recipientType",
                        input.recipientType().name()))
                .build());
    }

    /**
     * Lập yêu cầu ngoại lệ cho một điều kiện (a) → (d), (h) (FR-064): Hành chính, hoặc Kế toán với ý tiền của (h). Quản lý viện
     * duyệt; áp dụng ngay ngày duyệt thêm dòng ngoại lệ ({@link EndingExceptionHandler}).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.CREATE)
    @Transactional
    public ApprovalRequestView requestException(Target target, long id, String conditionCode, Reason reason) {
        ResidencyEnding ending = store.find(id).orElseThrow(NotFoundException::new);
        if (!ending.preparing()) {
            throw notPreparing();
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("residencyEndingId", id);
        content.put("conditionCode", conditionCode);
        return approvals
                .getObject()
                .create(new CreateRequest(EndingExceptionHandler.TYPE, content, time.today(), reason, true));
    }

    // ---- Dùng trong module -----------------------------------------------------------------------------------------

    /** Hành chính hoặc Bác sĩ quản lý hồ sơ kết thúc; Kế toán chỉ xem và lập ngoại lệ ý tiền (Phụ lục 27 chú thích 39). */
    static void requireOfficeOrDoctor(CurrentUser user) {
        if (!user.roles().contains(Role.OFFICE_STAFF) && !user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException("Chỉ Hành chính hoặc Bác sĩ thao tác hồ sơ kết thúc lưu trú (FR-062)");
        }
    }

    private static void requireCreateRole(CurrentUser user, NewEndingInput input) {
        if (user.roles().contains(Role.OFFICE_STAFF)) {
            return;
        }
        if (user.roles().contains(Role.DOCTOR) && input.medicalReason() && MEDICAL_CASES.contains(input.endCase())) {
            return;
        }
        throw new PermissionDeniedException(
                "Hành chính lập hồ sơ kết thúc; Bác sĩ chỉ lập Xuất viện, Chuyển cơ sở vì lý do y tế (FR-062)");
    }

    private ResidencyEnding lockedPreparing(long id) {
        ResidencyEnding found = store.find(id).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        ResidencyEnding ending = store.lock(id).orElseThrow(NotFoundException::new);
        if (!ending.preparing()) {
            throw notPreparing();
        }
        return ending;
    }

    static BusinessException notPreparing() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "RESIDENCY_ENDING_NOT_PREPARING",
                "Hồ sơ kết thúc lưu trú không còn Đang chuẩn bị (FR-067)");
    }

    private static void validate(HandoverInput input) {
        if (input.recipientType() == null || input.handoverAt() == null || input.handoverStaffId() <= 0) {
            throw new InvalidValueException("Bàn giao cần người nhận, thời điểm và nhân viên bàn giao (FR-063 (e))");
        }
        boolean family = input.recipientType() == RecipientType.FAMILY_MEMBER;
        if (family && (input.familyMemberId() == null || input.familyMemberId() <= 0)) {
            throw new InvalidValueException("Bàn giao cho người thân cần familyMemberId");
        }
        if (!family && (input.facilityName() == null || input.facilityName().isBlank())) {
            throw new InvalidValueException("Bàn giao cho cơ sở tiếp nhận cần facilityName");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (v1 != null) {
            map.put(k1, v1.toString());
        }
        if (v2 != null) {
            map.put(k2, v2.toString());
        }
        return map;
    }

    /** Mã điều kiện hợp lệ cho ngoại lệ (FR-064); sai thì 422. */
    static EndingConditionCode exceptionCode(String code) {
        try {
            EndingConditionCode parsed = EndingConditionCode.valueOf(code == null ? "" : code);
            if (!parsed.waivable()) {
                throw new InvalidValueException(
                        "Điều kiện " + code + " không có ngoại lệ (FR-064: bàn giao và xác nhận quyết toán)");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("conditionCode không hợp lệ: " + code);
        }
    }
}
