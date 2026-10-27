package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import com.cambae.backend.m02.internal.registration.domain.RegistrationObligation;
import com.cambae.backend.m02.internal.registration.domain.RegistrationStatus;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import com.cambae.backend.m02.internal.registration.domain.SubmissionResult;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationObligationStore;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationResidentStore;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationSubmissionStore;
import com.cambae.backend.m02.internal.registration.persistence.ResidenceRegistrationStore;
import com.cambae.backend.m02.internal.registration.service.ExpectedStayDuration.StayPeriod;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh và tra cứu khai báo tạm trú, lưu trú (018 US1, T008; FR-001 → FR-004, FR-009; BR-M02-11, DBR-33, Q-215, Q-306;
 * research Z-03, Z-04). Chỉ actor (Hành chính) lập khai báo, qua {@link #createRegistration}; Bộ lập lịch không tới được
 * phương thức này ({@code ResidenceRegistrationArchTest}). Mọi lệnh khóa {@code registration_resident} của người cao tuổi
 * trước rồi đọc khóa khai báo; chuyển trạng thái bằng câu lệnh có điều kiện trạng thái. Ghi nộp, kết quả (US2, T010) ở
 * Phase 4; khai báo gia hạn (T012), khai báo xóa (T014) thêm ở Phase 5, 6.
 */
@Service
public class ResidenceRegistrationService {

    static final AuditAction CREATED = new AuditAction("M02_REGISTRATION_CREATED", false);
    static final AuditAction LOCAL_CONFIRMED = new AuditAction("M02_REGISTRATION_LOCAL_CONFIRMED", false);
    static final AuditAction SUBMITTED = new AuditAction("M02_REGISTRATION_SUBMITTED", false);
    static final AuditAction ACCEPTED = new AuditAction("M02_REGISTRATION_ACCEPTED", false);
    static final AuditAction REJECTED = new AuditAction("M02_REGISTRATION_REJECTED", false);

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    private final ResidenceRegistrationStore registrations;
    private final RegistrationObligationStore obligations;
    private final RegistrationResidentStore residentLock;
    private final RegistrationSubmissionStore submissions;
    private final FileApi files;
    private final ContractReadApi contracts;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    ResidenceRegistrationService(
            ResidenceRegistrationStore registrations,
            RegistrationObligationStore obligations,
            RegistrationResidentStore residentLock,
            RegistrationSubmissionStore submissions,
            FileApi files,
            ContractReadApi contracts,
            ResidentApi residents,
            ParameterApi parameters,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.registrations = registrations;
        this.obligations = obligations;
        this.residentLock = residentLock;
        this.submissions = submissions;
        this.files = files;
        this.contracts = contracts;
        this.residents = residents;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
    }

    /**
     * Việc trong danh sách cần làm (api-018 {@code RegistrationTask}). Phase 3 trả {@code TO_CREATE}, {@code OVERDUE}; sắp
     * hết hạn, Hết hạn (T012), cần khai báo xóa (T014) trả từ Phase 5, 6.
     */
    public enum TaskType {
        TO_CREATE,
        OVERDUE,
        EXPIRING_SOON,
        EXPIRED,
        TO_DEREGISTER
    }

    public record Task(long residentId, String fullName, TaskType taskType, Instant dueAt, Long registrationId) {}

    /** Nội dung ghi đã nộp (FR-003): ngày nộp, kênh, cơ quan bắt buộc; mã hồ sơ không bắt buộc. */
    public record Submission(
            LocalDate submittedDate, SubmissionChannel channel, String receivingAuthority, String dossierCode) {}

    /** Nội dung ghi được chấp nhận (FR-003): bằng chứng; ngày hết hạn với Đăng ký tạm trú. */
    public record Acceptance(Long evidenceFileId, LocalDate expiryDate) {}

    public static Target targetForResident(long residentId) {
        return new Target(RegistrationNames.ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một khai báo; không có thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long id) {
        ResidenceRegistration registration = registrations.find(id).orElseThrow(NotFoundException::new);
        return RegistrationNames.target(id, registration.residentId());
    }

    // ---- Đọc ------------------------------------------------------------------------------------------------------

    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ResidenceRegistration> ofResident(Target target, long residentId) {
        return registrations.ofResident(residentId);
    }

    /**
     * Người cần khai báo, gồm người chưa được lập khai báo (FR-001, FR-002): nghĩa vụ chưa ghi nộp; quá hạn (hạn đang có
     * hiệu lực trước hôm nay; khai báo bị từ chối thì hạn mới của khai báo) là {@code OVERDUE} dù đã lập hay chưa, còn hạn mà
     * chưa lập là {@code TO_CREATE}.
     */
    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Task> toDo(Target target) {
        Instant todayStart = time.today().atStartOfDay(BusinessTime.ZONE).toInstant();
        List<Task> tasks = new ArrayList<>();
        for (RegistrationObligation obligation : obligations.pending()) {
            TaskType type;
            if (obligation.effectiveDueAt().isBefore(todayStart)) {
                type = TaskType.OVERDUE;
            } else if (obligation.registrationId() == null) {
                type = TaskType.TO_CREATE;
            } else {
                continue;
            }
            tasks.add(new Task(
                    obligation.residentId(),
                    residents.identity(obligation.residentId()).fullName(),
                    type,
                    obligation.effectiveDueAt(),
                    obligation.registrationId()));
        }
        return tasks;
    }

    // ---- Lệnh -----------------------------------------------------------------------------------------------------

    /**
     * Lập khai báo ở Cần khai báo (FR-001, FR-001a, FR-002, FR-004): người nội trú có nghĩa vụ chưa đáp ứng, chưa có khai
     * báo đang xử lý. Loại theo tổng thời gian lưu trú dự kiến (từ ngày hiệu lực phần nội trú với mốc FR-001a) và CFG-M02-11
     * đọc lúc lập; người đã xác nhận thường trú cùng xã/phường luôn Thông báo lưu trú. Hạn là hạn của nghĩa vụ sớm nhất;
     * mọi nghĩa vụ đang mở của người đó được đáp ứng. Khai báo Đã xác nhận đang có (nếu có) ghi là khai báo được thay thế:
     * cột đặt lúc lập (trigger V018_01 không cho đổi), khai báo cũ chỉ chuyển Được thay thế khi khai báo này được chấp nhận
     * (T010). Lập khai báo gia hạn (FR-006) thêm ở T012.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.CREATE)
    @Transactional
    public ResidenceRegistration createRegistration(Target target, long residentId) {
        boolean localPermanentResident = residentLock.lock(residentId);
        if (registrations.hasInProgress(residentId)) {
            throw inProgressExists();
        }
        List<RegistrationObligation> open = obligations.openOf(residentId);
        List<ContractView> signed = contracts.ofResident(residentId).stream()
                .filter(contract -> SIGNED.contains(contract.status()))
                .toList();
        if (open.isEmpty()) {
            if (!residentialToday(signed)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "NOT_RESIDENTIAL_CARE",
                        "Người bán trú không có khai báo tạm trú, lưu trú (FR-001)");
            }
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_REGISTRATION_OBLIGATION",
                    "Chưa phát sinh nghĩa vụ khai báo hoặc nghĩa vụ đã có khai báo (FR-001)");
        }
        RegistrationObligation obligation = open.getFirst();
        RegistrationType type = typeFor(obligation, signed, localPermanentResident);
        Long replaced = registrations.confirmedOf(residentId).orElse(null);

        long id;
        try {
            id = registrations.insertToSubmit(
                    residentId,
                    type,
                    false,
                    obligation.id(),
                    obligation.dueAt(),
                    localPermanentResident,
                    null,
                    replaced);
        } catch (DuplicateKeyException e) {
            // Khóa registration_resident đã chặn; khóa DBR-33 của cơ sở dữ liệu là lớp chặn cuối
            throw inProgressExists();
        }
        open.forEach(o -> obligations.markMet(o.id(), id));
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(RegistrationNames.ENTITY_TYPE, id)
                .resident(residentId)
                .after(Map.of(
                        "type", type.name(),
                        "status", RegistrationStatus.TO_SUBMIT.name(),
                        "dueAt", obligation.dueAt().toString(),
                        "obligationId", obligation.id()))
                .build());
        return registrations.find(id).orElseThrow();
    }

    /**
     * Xác nhận thường trú cùng xã/phường với viện (FR-001, Q-215): khi khai báo còn Cần khai báo, có ghi chú căn cứ; loại
     * chuyển Thông báo lưu trú, hạn giữ nguyên; cờ của hồ sơ làm mọi khai báo sau dùng loại này. Gỡ bằng đính chính (T010).
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.UPDATE)
    @Transactional
    public ResidenceRegistration confirmLocalPermanentResident(Target target, long id, String basisNote) {
        if (basisNote == null || basisNote.isBlank()) {
            throw new InvalidValueException("Cần ghi chú căn cứ khi xác nhận thường trú cùng xã/phường (FR-001)");
        }
        long residentId =
                registrations.find(id).orElseThrow(NotFoundException::new).residentId();
        residentLock.lock(residentId);
        ResidenceRegistration before = registrations.find(id).orElseThrow(NotFoundException::new);
        if (before.status() != RegistrationStatus.TO_SUBMIT
                || !registrations.confirmLocalPermanentResident(id, basisNote.strip())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STATE_CHANGED",
                    "Chỉ xác nhận thường trú cùng xã/phường khi khai báo còn Cần khai báo (FR-005)");
        }
        residentLock.confirmLocalPermanentResident(
                residentId, basisNote.strip(), currentUser.getObject().accountId(), time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(LOCAL_CONFIRMED)
                .entity(RegistrationNames.ENTITY_TYPE, id)
                .resident(residentId)
                .before(Map.of("type", before.type().name(), "localPermanentResident", false))
                .after(Map.of(
                        "type", RegistrationType.STAY_NOTIFICATION.name(),
                        "localPermanentResident", true,
                        "basisNote", basisNote.strip()))
                .build());
        return registrations.find(id).orElseThrow();
    }

    /**
     * Ghi đã nộp (FR-003, FR-005; US2 #1): Cần khai báo → Đã nộp; thêm lần nộp mới ({@code PENDING}), lần nộp cũ giữ
     * nguyên; nghĩa vụ của khai báo thôi nhắc quá hạn.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.UPDATE)
    @Transactional
    public ResidenceRegistration recordSubmitted(Target target, long id, Submission submission) {
        if (submission.submittedDate() == null
                || submission.channel() == null
                || isBlank(submission.receivingAuthority())) {
            throw new InvalidValueException("Cần ngày nộp, kênh nộp và cơ quan tiếp nhận (FR-005)");
        }
        ResidenceRegistration before = locked(id);
        if (before.status() != RegistrationStatus.TO_SUBMIT) {
            throw stateChanged("Chỉ ghi đã nộp khi khai báo ở Cần khai báo (FR-005)");
        }
        String dossierCode = isBlank(submission.dossierCode())
                ? null
                : submission.dossierCode().strip();
        long submissionId = submissions.add(
                id,
                submission.submittedDate(),
                submission.channel(),
                submission.receivingAuthority().strip(),
                dossierCode,
                currentUser.getObject().accountId(),
                time.now());
        if (!registrations.markSubmitted(id, submissionId)) {
            throw stateChanged("Chỉ ghi đã nộp khi khai báo ở Cần khai báo (FR-005)");
        }
        obligations.markSubmitted(id, true);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", RegistrationStatus.SUBMITTED.name());
        after.put("submissionId", submissionId);
        after.put("submittedDate", submission.submittedDate().toString());
        after.put("channel", submission.channel().name());
        after.put("receivingAuthority", submission.receivingAuthority().strip());
        after.put("dossierCode", dossierCode);
        audit(SUBMITTED, before, after);
        return registrations.find(id).orElseThrow();
    }

    /**
     * Ghi được chấp nhận (FR-003, FR-005; US2 #2, #5): Đã nộp → Đã xác nhận; bằng chứng bắt buộc, Đăng ký tạm trú có ngày
     * hết hạn (Thông báo lưu trú không có). Khai báo Đã xác nhận cũ của người đó chuyển Được thay thế trước, cùng
     * transaction (DBR-33).
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.UPDATE)
    @Transactional
    public ResidenceRegistration recordAccepted(Target target, long id, Acceptance acceptance) {
        ResidenceRegistration before = locked(id);
        requireSubmitted(before);
        if (acceptance.evidenceFileId() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "EVIDENCE_REQUIRED", "Cần bằng chứng khi ghi được chấp nhận (FR-003)");
        }
        boolean temporaryResidence = before.type() == RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION;
        if (temporaryResidence && acceptance.expiryDate() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "EXPIRY_DATE_REQUIRED",
                    "Đăng ký tạm trú được chấp nhận cần ngày hết hạn (FR-003)");
        }
        if (!temporaryResidence && acceptance.expiryDate() != null) {
            throw new InvalidValueException("Chỉ Đăng ký tạm trú có ngày hết hạn (FR-003)");
        }
        Long superseded = registrations
                .confirmedOf(before.residentId())
                .filter(confirmed -> confirmed != id)
                .orElse(null);
        if (superseded != null) {
            registrations.markSuperseded(superseded);
        }
        if (!submissions.setResult(before.currentSubmissionId(), SubmissionResult.ACCEPTED, null)
                || !registrations.markConfirmed(id, acceptance.expiryDate(), acceptance.evidenceFileId())) {
            throw stateChanged("Lần nộp đã có kết quả (FR-003)");
        }
        files.attach(acceptance.evidenceFileId(), RegistrationNames.EVIDENCE_OWNER_TYPE, id);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", RegistrationStatus.CONFIRMED.name());
        after.put("submissionId", before.currentSubmissionId());
        after.put("evidenceFileId", acceptance.evidenceFileId());
        after.put(
                "expiryDate",
                acceptance.expiryDate() == null ? null : acceptance.expiryDate().toString());
        after.put("supersededRegistrationId", superseded);
        audit(ACCEPTED, before, after);
        return registrations.find(id).orElseThrow();
    }

    /**
     * Ghi bị từ chối (FR-005; US2 #3): Đã nộp → Cần khai báo, cùng khai báo (không lập khai báo mới); hạn mới của khai báo
     * = thời điểm ghi + CFG-M02-12. Nghĩa vụ giữ hạn gốc và mốc đã báo Quản lý viện, chỉ đặt lại cờ đã ghi nộp để nhắc quá
     * hạn chạy lại theo hạn mới (plan 018 "Phase 4 hoàn thành").
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCE_REGISTRATION, operation = Operation.UPDATE)
    @Transactional
    public ResidenceRegistration recordRejected(Target target, long id, String reason) {
        if (isBlank(reason)) {
            throw new InvalidValueException("Cần lý do khi ghi bị từ chối (FR-005)");
        }
        ResidenceRegistration before = locked(id);
        requireSubmitted(before);
        Instant newDueAt =
                time.now().plus(parameters.read(ParameterCode.CFG_M02_12).asDuration());
        if (!submissions.setResult(before.currentSubmissionId(), SubmissionResult.REJECTED, reason.strip())
                || !registrations.markRejected(id, newDueAt)) {
            throw stateChanged("Lần nộp đã có kết quả (FR-003)");
        }
        obligations.markSubmitted(id, false);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", RegistrationStatus.TO_SUBMIT.name());
        after.put("submissionId", before.currentSubmissionId());
        after.put("rejectionReason", reason.strip());
        after.put("dueAt", newDueAt.toString());
        audit(REJECTED, before, after);
        return registrations.find(id).orElseThrow();
    }

    /**
     * Gỡ xác nhận thường trú cùng xã/phường nhầm (FR-001), tác động của đính chính {@code M02.RESIDENCE_REGISTRATION}
     * (chạy trong transaction của lệnh đính chính): chỉ khi khai báo còn Cần khai báo; loại tính lại như lúc lập, hạn giữ;
     * cờ của hồ sơ bỏ.
     */
    void removeLocalConfirmation(long id) {
        ResidenceRegistration before = locked(id);
        if (before.status() != RegistrationStatus.TO_SUBMIT || !before.localPermanentResident()) {
            throw stateChanged("Chỉ gỡ xác nhận thường trú cùng xã/phường khi khai báo còn Cần khai báo (FR-001)");
        }
        RegistrationObligation obligation =
                obligations.find(before.obligationId()).orElseThrow();
        List<ContractView> signed = contracts.ofResident(before.residentId()).stream()
                .filter(contract -> SIGNED.contains(contract.status()))
                .toList();
        if (!registrations.removeLocalPermanentResident(id, typeFor(obligation, signed, false))) {
            throw stateChanged("Chỉ gỡ xác nhận thường trú cùng xã/phường khi khai báo còn Cần khai báo (FR-001)");
        }
        residentLock.removeLocalPermanentResident(before.residentId());
    }

    /** Khóa người cao tuổi của khai báo rồi đọc khóa khai báo; không có thì 404. */
    private ResidenceRegistration locked(long id) {
        long residentId = registrations.residentOf(id).orElseThrow(NotFoundException::new);
        residentLock.lock(residentId);
        return registrations.findForUpdate(id).orElseThrow(NotFoundException::new);
    }

    private static void requireSubmitted(ResidenceRegistration registration) {
        if (registration.status() == RegistrationStatus.TO_SUBMIT) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOT_SUBMITTED", "Chưa ghi đã nộp nên chưa ghi được kết quả (US2 #4)");
        }
        if (registration.status() != RegistrationStatus.SUBMITTED) {
            throw stateChanged("Chỉ ghi kết quả khi khai báo ở Đã nộp (FR-005)");
        }
    }

    /** Loại khai báo theo FR-001, FR-001a: tổng thời gian lưu trú dự kiến, CFG-M02-11 đọc lúc gọi, cờ thường trú. */
    private RegistrationType typeFor(
            RegistrationObligation obligation, List<ContractView> signed, boolean localPermanentResident) {
        LocalDate residentialFrom = obligation.trigger() == ObligationTrigger.RESIDENTIAL_EFFECTIVE
                ? obligation.triggerAt().atZone(BusinessTime.ZONE).toLocalDate()
                : null;
        ExpectedStayDuration stay = ExpectedStayDuration.of(
                signed.stream().map(ResidenceRegistrationService::period).toList(), residentialFrom);
        int thresholdDays = Math.toIntExact(
                parameters.read(ParameterCode.CFG_M02_11).asDuration().toDays());
        return RegistrationTypeRule.determine(stay, thresholdDays, localPermanentResident);
    }

    private void audit(AuditAction action, ResidenceRegistration before, Map<String, Object> after) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(RegistrationNames.ENTITY_TYPE, before.id())
                .resident(before.residentId())
                .before(Map.of(
                        "status",
                        before.status().name(),
                        "dueAt",
                        before.dueAt().toString()))
                .after(after)
                .build());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException stateChanged(String detail) {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", detail);
    }

    private static BusinessException inProgressExists() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "REGISTRATION_IN_PROGRESS_EXISTS",
                "Người cao tuổi đã có khai báo Cần khai báo hoặc Đã nộp (FR-004, DBR-33)");
    }

    /** Hợp đồng: ngày kết thúc hiện hành (gồm phụ lục gia hạn), không có thì ngày kết thúc ghi trên hợp đồng. */
    private static StayPeriod period(ContractView contract) {
        LocalDate end = contract.currentEndDate() != null ? contract.currentEndDate() : contract.endDate();
        return new StayPeriod(contract.startDate(), end);
    }

    /** Hợp đồng đã ký bao hôm nay (hoặc bắt đầu muộn nhất nếu không có) là nội trú. */
    private boolean residentialToday(List<ContractView> signed) {
        LocalDate today = time.today();
        Comparator<ContractView> latest = Comparator.comparing(ContractView::startDate);
        return signed.stream()
                .filter(c -> !c.startDate().isAfter(today))
                .filter(c -> period(c).end() == null || !period(c).end().isBefore(today))
                .max(latest)
                .or(() -> signed.stream().max(latest))
                .map(c -> c.residenceType() != ResidenceType.DAY_CARE)
                .orElse(false);
    }
}
