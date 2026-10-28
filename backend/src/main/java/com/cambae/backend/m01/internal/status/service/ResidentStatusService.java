package com.cambae.backend.m01.internal.status.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileView;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ConditionWaiver;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.api.events.ResidentStatusChanged;
import com.cambae.backend.m01.api.events.ResidentStatusNotice;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m01.internal.health.service.ActorNames;
import com.cambae.backend.m01.internal.profile.domain.Resident;
import com.cambae.backend.m01.internal.profile.persistence.ResidentRepository;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.status.domain.ResidentStatusHistory;
import com.cambae.backend.m01.internal.status.domain.StatusTransitionTable;
import com.cambae.backend.m01.internal.status.persistence.ResidentStatusHistoryRepository;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Điểm duy nhất đổi trạng thái người cao tuổi (001 US4; FR-044 → FR-050; research H-02, H-03). Mỗi lệnh: khóa dòng hồ
 * sơ, chặn hồ sơ trạng thái cuối, kiểm bảng chuyển, gộp điều kiện của mọi module tham gia, ghi lịch sử và nhật ký, áp
 * tác động của mọi module trong cùng transaction rồi phát sự kiện. Một bước lỗi thì cả lệnh rollback (FR-047).
 */
@Service
public class ResidentStatusService {

    static final AuditAction CHANGED = new AuditAction("RESIDENT_STATUS_CHANGED", false);
    static final String ENTITY_TYPE = "M01.RESIDENT";
    /** Loại bản ghi của dòng lịch sử trạng thái trong đính chính của 000 (FR-049a). */
    public static final String HISTORY_RECORD_TYPE = "M01.RESIDENT_STATUS_HISTORY";

    /** Chi tiết bắt buộc của Chuyển viện và Ghi nhận qua đời ngoài viện (CHK016, FR-047b). */
    private static final String BASIS_FIELD = "basis";

    private static final String EVIDENCE_FIELD = "evidenceFileId";

    private static final String EVIDENCE_BASIS = "EVIDENCE_FILE";

    /** Chủ sở hữu của tệp bằng chứng qua đời là dòng lịch sử trạng thái; quyền tải ở {@code DeathEvidenceFilePermission}. */
    static final String DEATH_EVIDENCE_OWNER = "M01.DEATH_EVIDENCE";

    /** Giấy tờ bệnh viện gắn vào dòng lịch sử của Ghi nhận trở về từ bệnh viện và Chuyển viện (FR-047c, research H-18). */
    static final String HOSPITAL_DOCUMENT_OWNER = "M01.HOSPITAL_DOCUMENT";

    private static final String ATTACHMENTS_FIELD = "attachmentFileIds";

    private static final int MAX_ATTACHMENTS = 10;

    /** Lệnh các module khác được gọi qua {@code ResidentStatusApi} (research H-03; 004 FR-002). */
    private static final Set<StatusCommand> MODULE_COMMANDS = Set.of(
            StatusCommand.ROLL_CALL_DEPARTURE,
            StatusCommand.ROLL_CALL_RETURN,
            StatusCommand.CANCEL_DEPARTURE_RECORD,
            StatusCommand.TRANSFER_OUT,
            StatusCommand.CANCEL_ADMISSION);

    private static final Set<StatusCommand> USER_COMMANDS = java.util.EnumSet.of(
            StatusCommand.COMPLETE_ADMISSION,
            StatusCommand.CANCEL_ADMISSION,
            StatusCommand.GRANT_TEMPORARY_ABSENCE,
            StatusCommand.RECORD_RETURN,
            StatusCommand.TRANSFER_OUT,
            StatusCommand.END_RESIDENCY,
            StatusCommand.RECORD_DEATH);

    /** {@code residentId} cho giới hạn trường theo người cao tuổi của dòng (không ghi ra JSON). */
    public record HistoryEntry(
            long residentId,
            ResidentStatus from,
            ResidentStatus to,
            String command,
            Instant at,
            String performedBy,
            boolean bySystem,
            String reason,
            Map<String, Object> basis,
            Map<String, Object> conditionWaiver) {}

    public record Outcome(ResidentStatus status, long historyId, HistoryEntry history, List<Warning> warnings) {}

    private final ResidentRepository residents;
    private final ResidentStatusHistoryRepository histories;
    private final ProfileOpenGuard guard;
    private final ObjectProvider<StatusTransitionParticipant> participants;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final JsonMapper json;
    private final ActorNames names;
    private final ObjectProvider<CurrentUser> currentUser;
    private final FileApi files;
    private final ObjectProvider<CorrectionApi> corrections;

    ResidentStatusService(
            ResidentRepository residents,
            ResidentStatusHistoryRepository histories,
            ProfileOpenGuard guard,
            ObjectProvider<StatusTransitionParticipant> participants,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            JsonMapper json,
            ActorNames names,
            ObjectProvider<CurrentUser> currentUser,
            FileApi files,
            ObjectProvider<CorrectionApi> corrections) {
        this.corrections = corrections;
        this.residents = residents;
        this.histories = histories;
        this.guard = guard;
        this.participants = participants;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.json = json;
        this.names = names;
        this.currentUser = currentUser;
        this.files = files;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.CREATE)
    @Transactional
    public Outcome completeAdmission(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(
                resident,
                StatusCommand.COMPLETE_ADMISSION,
                currentUser.getObject(),
                Optional.of(reason),
                details,
                null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.CREATE)
    @Transactional
    public Outcome cancelAdmission(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(
                resident, StatusCommand.CANCEL_ADMISSION, currentUser.getObject(), Optional.of(reason), details, null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public Outcome grantTemporaryAbsence(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(
                resident,
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                currentUser.getObject(),
                Optional.of(reason),
                details,
                null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ABSENCE_RETURN, operation = Operation.CREATE)
    @Transactional
    public Outcome recordReturn(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(
                resident, StatusCommand.RECORD_RETURN, currentUser.getObject(), Optional.of(reason), details, null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.TRANSFER_OUT, operation = Operation.CREATE)
    @Transactional
    public Outcome transferOut(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(resident, StatusCommand.TRANSFER_OUT, currentUser.getObject(), Optional.of(reason), details, null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.CREATE)
    @Transactional
    public Outcome endResidency(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(
                resident, StatusCommand.END_RESIDENCY, currentUser.getObject(), Optional.of(reason), details, null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.CREATE)
    @Transactional
    public Outcome recordDeath(Target target, Reason reason, JsonNode details) {
        Resident resident = lock(target);
        guard.requireOpen(resident.getId());
        return apply(resident, StatusCommand.RECORD_DEATH, currentUser.getObject(), Optional.of(reason), details, null);
    }

    /**
     * Lệnh do module khác gọi trong transaction của lệnh nguồn (research H-03): vẫn chạy bảng chuyển, khóa dòng, điều
     * kiện, tác động. Quyền do module nguồn kiểm.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TransitionResult executeFromModule(
            long residentId,
            StatusCommand command,
            CurrentUser user,
            Optional<Reason> reason,
            JsonNode details,
            Basis basis) {
        if (!MODULE_COMMANDS.contains(command)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "COMMAND_NOT_ALLOWED_FOR_MODULE",
                    "Lệnh " + command + " chỉ thực hiện qua endpoint của M01");
        }
        Resident resident = residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        Outcome outcome = apply(resident, command, user, reason, details, basis);
        return new TransitionResult(
                residentId,
                outcome.history().from(),
                outcome.status(),
                command,
                outcome.historyId(),
                outcome.history().at(),
                details);
    }

    /** Xem trước điều kiện của một lệnh, cùng bộ kiểm tra với lệnh thật nhưng không đổi dữ liệu (FR-045). */
    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Condition> previewConditions(Target target, StatusCommand command) {
        return preview(target.residentId(), command);
    }

    @Transactional(readOnly = true)
    public List<Condition> preview(long residentId, StatusCommand command) {
        Resident resident = residents.findById(residentId).orElseThrow(NotFoundException::new);
        requireTransition(resident.getStatus(), command);
        CommandContext context = context(resident, command, currentUser.getIfAvailable(), noDetails());
        return evaluate(context).unmet();
    }

    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Target target) {
        long id = target.residentId();
        residents.findById(id).orElseThrow(NotFoundException::new);
        List<ResidentStatusHistory> rows = histories.findByResidentIdOrderByIdAsc(id);
        // Lý do hiển thị là lý do đã đính chính (FR-049a, FR-030); bản gốc giữ nguyên ở bảng lịch sử
        Map<Long, CurrentValue> corrected = rows.isEmpty()
                ? Map.of()
                : corrections
                        .getObject()
                        .current(
                                HISTORY_RECORD_TYPE,
                                rows.stream().map(ResidentStatusHistory::getId).toList());
        return rows.stream()
                .map(row -> entry(row, correctedReason(corrected.get(row.getId()))))
                .toList();
    }

    /** Giấy tờ bệnh viện gắn với một dòng lịch sử trạng thái (001 T081, T082). */
    public List<FileView> hospitalDocuments(long historyId) {
        return files.attachedTo(HOSPITAL_DOCUMENT_OWNER, historyId);
    }

    /**
     * Lệnh người dùng hợp trạng thái hiện tại và vai trò, để giao diện chỉ hiện nút dùng được: cặp ngoài bảng chuyển bị
     * loại, Ghi nhận qua đời tại viện chỉ Bác sĩ (FR-047b).
     */
    public static Set<StatusCommand> availableCommands(ResidentStatus status, Set<Role> roles) {
        Set<StatusCommand> available = new java.util.HashSet<>();
        for (StatusCommand command : USER_COMMANDS) {
            if (StatusTransitionTable.next(status, command).isPresent()) {
                available.add(command);
            }
        }
        if (status == ResidentStatus.IN_RESIDENCE && !roles.contains(Role.DOCTOR)) {
            available.remove(StatusCommand.RECORD_DEATH);
        }
        return available;
    }

    public static Target target(long residentId) {
        return ResidentProfileService.target(residentId);
    }

    private Resident lock(Target target) {
        return residents.lockById(target.residentId()).orElseThrow(NotFoundException::new);
    }

    private Outcome apply(
            Resident resident,
            StatusCommand command,
            CurrentUser user,
            Optional<Reason> reason,
            JsonNode details,
            Basis moduleBasis) {
        long residentId = resident.getId();
        ResidentStatus from = resident.getStatus();
        ResidentStatus to = requireTransition(from, command);
        boolean bySystem = StatusTransitionTable.SYSTEM_COMMANDS.contains(command);
        if (!bySystem && reason.isEmpty()) {
            throw new ReasonRequiredException();
        }
        JsonNode body = details == null ? noDetails() : details;
        BasisRef basis = basisOf(command, from, user, body, moduleBasis);
        List<Long> attachments = attachmentsOf(command, from, body);
        participants
                .orderedStream()
                .filter(p -> p.interestedCommands().contains(command))
                .forEach(p -> p.validateDetails(command, body));

        Instant now = time.now();
        CommandContext context = context(resident, command, user, body);
        Evaluation evaluation = evaluate(context);
        if (!evaluation.unmet().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNMET_CONDITIONS",
                    "Còn điều kiện chưa thỏa, chưa chuyển trạng thái được",
                    Map.of("unmetConditions", evaluation.unmet()));
        }

        ResidentStatusHistory row = histories.append(ResidentStatusHistory.transition(
                residentId,
                from,
                to,
                command.name(),
                now,
                user.accountId(),
                bySystem,
                reason.map(Reason::value).orElse(null),
                basis == null ? null : basis.type(),
                basis == null ? null : basis.id(),
                evaluation.waiver() == null ? null : json.writeValueAsString(evaluation.waiver())));
        // tệp không có hoặc đã gắn thì ném lỗi và cả lệnh rollback, kể cả tệp đã gắn trước đó trong vòng lặp (FR-047c)
        attachments.forEach(fileId -> files.attach(fileId, HOSPITAL_DOCUMENT_OWNER, row.getId()));
        if (basis != null && EVIDENCE_BASIS.equals(basis.type())) {
            // tệp không tồn tại hoặc đã gắn thì ném lỗi và cả lệnh rollback (FR-047b, research H-13)
            files.attach(basis.id(), DEATH_EVIDENCE_OWNER, row.getId());
        }
        resident.changeStatus(to, now);
        residents.saveAndFlush(resident);

        auditLog.write(AuditLogEntry.builder()
                .action(CHANGED)
                .entity(ENTITY_TYPE, residentId)
                .resident(residentId)
                .before(Map.of("status", from.name()))
                .after(Map.of("status", to.name()))
                .reason(reason.orElse(null))
                .basis(moduleBasis)
                .details(Map.of("command", command.name(), "historyId", row.getId()))
                .build());

        TransitionResult result = new TransitionResult(residentId, from, to, command, row.getId(), now, body);
        List<Warning> warnings = new ArrayList<>();
        participants
                .orderedStream()
                .filter(p -> p.interestedCommands().contains(command))
                .forEach(p -> {
                    p.applyEffects(result);
                    warnings.addAll(p.warnings(context));
                });
        events.publishEvent(new ResidentStatusChanged(residentId, from, to, command, row.getId(), now));
        if (command == StatusCommand.TRANSFER_OUT || command == StatusCommand.RECORD_DEATH) {
            events.publishEvent(new ResidentStatusNotice(residentId, command, row.getId(), now));
        }
        return new Outcome(to, row.getId(), entry(row), List.copyOf(warnings));
    }

    private static ResidentStatus requireTransition(ResidentStatus from, StatusCommand command) {
        return StatusTransitionTable.next(from, command)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_TRANSITION",
                        "Không chuyển được từ " + from + " bằng lệnh " + command));
    }

    private CommandContext context(Resident resident, StatusCommand command, CurrentUser user, JsonNode details) {
        return new CommandContext(resident.getId(), resident.getStatus(), command, user, time.now(), details);
    }

    private record Evaluation(List<Condition> unmet, Map<String, Object> waiver) {}

    /** Gộp điều kiện chưa thỏa của mọi module quan tâm lệnh, rồi bỏ những điều kiện miễn được mà 004 đã được duyệt miễn. */
    private Evaluation evaluate(CommandContext context) {
        List<StatusTransitionParticipant> interested = participants
                .orderedStream()
                .filter(p -> p.interestedCommands().contains(context.command()))
                .toList();
        List<Condition> unmet = new ArrayList<>();
        interested.forEach(p -> unmet.addAll(p.checkConditions(context)));
        if (unmet.isEmpty()) {
            return new Evaluation(List.of(), null);
        }
        Optional<ConditionWaiver> waiver = interested.stream()
                .map(p -> p.waiveConditions(context))
                .flatMap(Optional::stream)
                .findFirst();
        if (waiver.isEmpty()) {
            return new Evaluation(List.copyOf(unmet), null);
        }
        Set<String> covered = waiver.get().conditionCodes();
        List<Condition> waived = unmet.stream()
                .filter(c -> c.waivable() && covered.contains(c.code()))
                .toList();
        List<Condition> remaining =
                unmet.stream().filter(c -> !waived.contains(c)).toList();
        if (waived.isEmpty()) {
            return new Evaluation(remaining, null);
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("approvalRequestId", waiver.get().approvalRequestId());
        record.put("conditionCodes", waived.stream().map(Condition::code).toList());
        return new Evaluation(remaining, record);
    }

    private record BasisRef(String type, long id) {}

    /**
     * Căn cứ ghi vào lịch sử và quy tắc riêng của từng lệnh người dùng: Chuyển viện nêu căn cứ (CHK016), Ghi nhận qua
     * đời do Bác sĩ khi còn ở viện, Hành chính chỉ khi ngoài viện và kèm giấy tờ bằng chứng (FR-047b, Q-342).
     */
    private BasisRef basisOf(
            StatusCommand command, ResidentStatus from, CurrentUser user, JsonNode details, Basis moduleBasis) {
        if (moduleBasis != null) {
            if (moduleBasis.approvalRequestId() != null) {
                return new BasisRef("APPROVAL_REQUEST", moduleBasis.approvalRequestId());
            }
            if (moduleBasis.sourceRecordId() != null) {
                return new BasisRef("SOURCE_RECORD", moduleBasis.sourceRecordId());
            }
            if (moduleBasis.schedulerRunId() != null) {
                return new BasisRef("SCHEDULER_RUN", moduleBasis.schedulerRunId());
            }
            return null;
        }
        if (command == StatusCommand.TRANSFER_OUT) {
            JsonNode basis = details.get(BASIS_FIELD);
            if (basis == null || basis.isNull() || (basis.isObject() && basis.isEmpty())) {
                throw invalidDetails(
                        "BASIS_REQUIRED",
                        "Chuyển viện cần nêu căn cứ (sự cố, chỉ định hoặc thông tin gia đình, cơ sở y tế)");
            }
            return null;
        }
        if (command == StatusCommand.RECORD_DEATH) {
            boolean doctor = user.roles().contains(Role.DOCTOR);
            JsonNode evidence = details.get(EVIDENCE_FIELD);
            boolean hasEvidence = evidence != null && evidence.isIntegralNumber();
            if (from == ResidentStatus.IN_RESIDENCE && !doctor) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "DEATH_AT_FACILITY_NEEDS_DOCTOR",
                        "Người cao tuổi mất tại viện phải do Bác sĩ xác nhận (FR-047b)");
            }
            if (!doctor && !hasEvidence) {
                throw invalidDetails(
                        "EVIDENCE_REQUIRED",
                        "Hành chính ghi nhận qua đời ngoài viện phải kèm giấy tờ bằng chứng (FR-047b)");
            }
            return hasEvidence ? new BasisRef(EVIDENCE_BASIS, evidence.longValue()) : null;
        }
        return null;
    }

    /**
     * Giấy tờ bệnh viện đính kèm (FR-047c): chỉ Ghi nhận trở về từ Điều trị tại bệnh viện và Chuyển viện nhận, không bắt
     * buộc, tối đa {@value #MAX_ATTACHMENTS} tệp; lệnh khác có tệp thì 422, và một tệp liệt kê hai lần chỉ gắn một lần.
     */
    private static List<Long> attachmentsOf(StatusCommand command, ResidentStatus from, JsonNode details) {
        JsonNode listed = details.get(ATTACHMENTS_FIELD);
        if (listed == null || listed.isNull() || (listed.isArray() && listed.isEmpty())) {
            return List.of();
        }
        boolean accepted = command == StatusCommand.TRANSFER_OUT
                || (command == StatusCommand.RECORD_RETURN && from == ResidentStatus.HOSPITALIZED);
        if (!accepted) {
            throw invalidDetails("ATTACHMENTS_NOT_ACCEPTED", "Lệnh này không nhận tệp đính kèm");
        }
        if (!listed.isArray() || listed.valueStream().anyMatch(id -> !id.isIntegralNumber())) {
            throw invalidDetails("INVALID_ATTACHMENTS", "Tệp đính kèm là danh sách mã tệp");
        }
        if (listed.size() > MAX_ATTACHMENTS) {
            throw invalidDetails("TOO_MANY_ATTACHMENTS", "Tối đa " + MAX_ATTACHMENTS + " tệp đính kèm");
        }
        return listed.valueStream().map(JsonNode::longValue).distinct().toList();
    }

    private static BusinessException invalidDetails(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    private static JsonNode noDetails() {
        return tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    }

    private static String correctedReason(CurrentValue value) {
        if (value != null && value.corrected() && !value.voided() && value.content() instanceof Map<?, ?> content) {
            Object reason = content.get(StatusHistoryCorrectionProvider.REASON);
            return reason == null ? null : reason.toString();
        }
        return null;
    }

    private HistoryEntry entry(ResidentStatusHistory row) {
        return entry(row, null);
    }

    private HistoryEntry entry(ResidentStatusHistory row, String correctedReason) {
        Map<String, Object> basis =
                row.getBasisType() == null ? null : Map.of("type", row.getBasisType(), "id", row.getBasisId());
        Map<String, Object> waiver = row.getConditionWaiver() == null
                ? null
                : json.readValue(row.getConditionWaiver(), new tools.jackson.core.type.TypeReference<>() {});
        return new HistoryEntry(
                row.getResidentId(),
                row.getFromStatus(),
                row.getToStatus(),
                row.getCommand(),
                row.getOccurredAt(),
                names.of(row.getPerformedBy()),
                row.isBySystem(),
                correctedReason != null ? correctedReason : row.getReason(),
                basis,
                waiver);
    }
}
