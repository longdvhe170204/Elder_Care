package com.cambae.backend.m01.internal.assessment.service;

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
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.CareLevelBasisType;
import com.cambae.backend.m01.api.dto.CareNeedView;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.AssessmentChangedCareLevelOrNeeds;
import com.cambae.backend.m01.api.events.CareLevelChangeRequestNeeded;
import com.cambae.backend.m01.api.events.CareLevelChanged;
import com.cambae.backend.m01.api.events.InitialAssessmentConfirmed;
import com.cambae.backend.m01.api.spi.CareLevelChangeRequestApi;
import com.cambae.backend.m01.internal.assessment.domain.Assessment;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.persistence.AssessmentRepository;
import com.cambae.backend.m01.internal.assessment.persistence.CareLevelHistoryStore;
import com.cambae.backend.m01.internal.assessment.service.AssessmentContents.Effective;
import com.cambae.backend.m01.internal.health.domain.CareNeed;
import com.cambae.backend.m01.internal.health.persistence.CareNeedRepository;
import com.cambae.backend.m01.internal.health.service.ActorNames;
import com.cambae.backend.m01.internal.profile.persistence.ResidentRepository;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.status.persistence.ResidentStatusStore;
import com.cambae.backend.m01.internal.status.service.ProfileOpenGuard;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lần đánh giá của người cao tuổi (001 US3; FR-024 → FR-038; research H-10). Điều dưỡng và Bác sĩ lập và sửa nháp, chỉ
 * Bác sĩ xác nhận; xác nhận đặt mức hiện hành, ghi lịch sử, báo module khác trong cùng transaction. Không có điểm, mức
 * đề xuất hay bản nháp kế hoạch chăm sóc (FR-024, FR-031).
 */
@Service
public class AssessmentService {

    public static final String RECORD_TYPE = "M01.ASSESSMENT";

    static final AuditAction CREATED = new AuditAction("ASSESSMENT_CREATED", false);
    static final AuditAction DRAFT_EDITED = new AuditAction("ASSESSMENT_DRAFT_EDITED", false);
    static final AuditAction CONFIRMED = new AuditAction("ASSESSMENT_CONFIRMED", false);
    static final AuditAction DISCARDED = new AuditAction("ASSESSMENT_DISCARDED", true);
    private static final int MAX_CARE_NOTE_LENGTH = 1000;
    private static final int MAX_BASIS_LENGTH = 1000;

    /** Trạng thái người cao tuổi cho phép đánh giá lại (FR-035), không kể Đang tiếp nhận có đầu vào xác nhận. */
    private static final Set<ResidentStatus> REASSESSABLE = EnumSet.of(
            ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.OFF_SITE_ACTIVITY);

    /** Nội dung lần đánh giá cho client, đã tính đính chính; {@code version} là ETag của bản gốc. */
    public record AssessmentView(
            long id,
            long residentId,
            AssessmentType type,
            String basis,
            String createdBy,
            String comment,
            String careNote,
            CareLevel careLevel,
            List<CareNeedView> careNeeds,
            ResidenceType proposedResidenceType,
            AssessmentStatus status,
            String confirmedBy,
            Instant confirmedAt,
            boolean corrected,
            int version) {}

    /** Nội dung gửi khi sửa nháp; thay toàn bộ các trường (FR-025). */
    public record DraftContent(
            String basis,
            String comment,
            String careNote,
            CareLevel careLevel,
            Set<Long> careNeedIds,
            ResidenceType proposedResidenceType) {}

    public record Effects(
            boolean careLevelChanged,
            List<Long> supersededRequestIds,
            boolean administratorNotified,
            boolean carePlanReviewNotified) {}

    public record ConfirmResult(AssessmentView assessment, Effects effects) {}

    private final AssessmentRepository assessments;
    private final ResidentRepository residents;
    private final ResidentStatusStore statuses;
    private final CareNeedRepository careNeeds;
    private final CareLevelHistoryStore history;
    private final AssessmentContents contents;
    private final ProfileOpenGuard guard;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ActorNames names;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<CareLevelChangeRequestApi> changeRequests;
    private final ObjectProvider<CurrentUser> currentUser;
    private final CareLevelChangeRequestReminderService changeRequestReminders;

    AssessmentService(
            AssessmentRepository assessments,
            ResidentRepository residents,
            ResidentStatusStore statuses,
            CareNeedRepository careNeeds,
            CareLevelHistoryStore history,
            AssessmentContents contents,
            ProfileOpenGuard guard,
            AuditLogApi auditLog,
            BusinessTime time,
            ActorNames names,
            ApplicationEventPublisher events,
            ObjectProvider<CareLevelChangeRequestApi> changeRequests,
            ObjectProvider<CurrentUser> currentUser,
            CareLevelChangeRequestReminderService changeRequestReminders) {
        this.assessments = assessments;
        this.residents = residents;
        this.statuses = statuses;
        this.careNeeds = careNeeds;
        this.history = history;
        this.contents = contents;
        this.guard = guard;
        this.auditLog = auditLog;
        this.time = time;
        this.names = names;
        this.events = events;
        this.changeRequests = changeRequests;
        this.currentUser = currentUser;
        this.changeRequestReminders = changeRequestReminders;
    }

    /** Đối tượng kiểm quyền của hồ sơ chứa lần đánh giá (đường dẫn {@code /assessments/{id}} không nêu hồ sơ). */
    @Transactional(readOnly = true)
    public Target targetOf(long assessmentId) {
        return ResidentProfileService.target(assessments
                .findById(assessmentId)
                .orElseThrow(NotFoundException::new)
                .getResidentId());
    }

    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<AssessmentView> list(Target target) {
        return views(assessments.findByResidentIdOrderByIdDesc(target.residentId()));
    }

    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public AssessmentView get(Target target, long id) {
        return views(List.of(find(target, id))).getFirst();
    }

    /** Lập nháp; tối đa một nháp mỗi người (BR-M01-13), loại và trạng thái theo FR-034, FR-035. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.CREATE)
    @Transactional
    public AssessmentView create(Target target, AssessmentType type, String basis) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        requireAssessable(residentId, type);
        if (assessments
                .findFirstByResidentIdAndStatus(residentId, AssessmentStatus.DRAFT)
                .isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "DRAFT_ALREADY_EXISTS", "Người cao tuổi đã có một bản nháp đánh giá");
        }
        Assessment created = assessments.saveAndFlush(new Assessment(
                residentId,
                type,
                normalizedBasis(basis),
                currentUser.getObject().accountId(),
                time.now()));
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(RECORD_TYPE, created.getId())
                .resident(residentId)
                .after(Map.of("type", type.name()))
                .build());
        return views(List.of(created)).getFirst();
    }

    /** Sửa bản nháp kèm {@code If-Match}; người tạo hoặc Bác sĩ. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.UPDATE)
    @Transactional
    public AssessmentView saveDraft(Target target, long id, String ifMatch, DraftContent input) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        Assessment a = find(target, id);
        requireAuthorOrDoctor(a);
        requireDraft(a);
        VersionEtag.requireMatch(ifMatch, a.getVersion(), views(List.of(a)).getFirst());
        DraftContent content = normalized(a, input);
        Map<String, Object> before = snapshot(a);
        a.editDraft(
                content.basis(),
                content.comment(),
                content.careNote(),
                content.careLevel(),
                content.careNeedIds(),
                content.proposedResidenceType());
        assessments.saveAndFlush(a);
        auditLog.write(AuditLogEntry.builder()
                .action(DRAFT_EDITED)
                .entity(RECORD_TYPE, id)
                .resident(residentId)
                .before(before)
                .after(snapshot(a))
                .build());
        return views(List.of(a)).getFirst();
    }

    /** Chỉ Bác sĩ xác nhận; tác động theo research H-10 trong cùng transaction. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.APPROVE)
    @Transactional
    public ConfirmResult confirm(Target target, long id, String ifMatch) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        Assessment a = find(target, id);
        requireDraft(a);
        VersionEtag.requireMatch(ifMatch, a.getVersion(), views(List.of(a)).getFirst());
        requireComplete(a);
        requireAssessable(residentId, a.getType());

        Optional<Assessment> previous = assessments.findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
                residentId, AssessmentStatus.CONFIRMED);
        Optional<CareLevel> currentLevel = history.current(residentId);
        ResidentStatus status = statuses.find(residentId).orElseThrow(NotFoundException::new);
        long user = currentUser.getObject().accountId();
        Instant now = time.now();
        CareLevel level = a.getCareLevel();

        a.confirm(user, now);
        assessments.saveAndFlush(a);

        boolean levelDiffers = currentLevel.map(l -> l != level).orElse(true);
        boolean changedNow = false;
        boolean administratorNotified = false;
        List<Long> superseded = List.of();
        if (a.getType() == AssessmentType.INITIAL || status == ResidentStatus.ADMISSION_IN_PROGRESS) {
            // Đầu vào, hoặc đánh giá lại khi Đang tiếp nhận (chưa có lưu trú): mức mới thay mức hiện hành ngay (FR-033,
            // FR-035)
            if (levelDiffers) {
                history.apply(residentId, level, now, CareLevelBasisType.ASSESSMENT, id, now, user);
                events.publishEvent(
                        new CareLevelChanged(residentId, currentLevel, level, now, CareLevelBasisType.ASSESSMENT, id));
                changedNow = true;
            }
            if (a.getType() == AssessmentType.INITIAL) {
                events.publishEvent(new InitialAssessmentConfirmed(residentId, id, level));
            }
        } else {
            // Đã lưu trú: không đổi mức; yêu cầu đổi mức chưa kết thúc bị thay thế, Hành chính lập yêu cầu mới (FR-036)
            CareLevelChangeRequestApi requests = changeRequests.getIfAvailable();
            if (requests != null) {
                superseded = requests.supersedeByAssessment(residentId, id);
            }
            if (levelDiffers) {
                events.publishEvent(new CareLevelChangeRequestNeeded(residentId, id, level));
                changeRequestReminders.startReminding(residentId, id);
                administratorNotified = true;
            }
        }

        boolean planReview = false;
        if (previous.isPresent()) {
            Assessment before = previous.get();
            CareLevel previousLevel = contents.of(before).careLevel();
            Set<Long> previousNeeds = before.getCareNeedIds();
            if (previousLevel != level || !previousNeeds.equals(a.getCareNeedIds())) {
                events.publishEvent(new AssessmentChangedCareLevelOrNeeds(
                        residentId, id, previousLevel, level, previousNeeds, a.getCareNeedIds()));
                planReview = true;
            }
        }

        auditLog.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(RECORD_TYPE, id)
                .resident(residentId)
                .after(Map.of("careLevel", level.name(), "confirmedAt", now.toString()))
                .build());
        return new ConfirmResult(
                views(List.of(a)).getFirst(), new Effects(changedNow, superseded, administratorNotified, planReview));
    }

    /** Hủy nháp kèm lý do; người tạo hoặc Bác sĩ. Bản đã hủy vẫn lưu, không sửa được (FR-026). */
    @BusinessCommand
    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.UPDATE)
    @Transactional
    public AssessmentView discard(Target target, long id, Reason reason) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        Assessment a = find(target, id);
        requireAuthorOrDoctor(a);
        requireDraft(a);
        a.discard(currentUser.getObject().accountId(), time.now(), reason.value());
        assessments.saveAndFlush(a);
        auditLog.write(AuditLogEntry.builder()
                .action(DISCARDED)
                .entity(RECORD_TYPE, id)
                .resident(residentId)
                .reason(reason)
                .build());
        return views(List.of(a)).getFirst();
    }

    private Assessment find(Target target, long id) {
        Assessment a = assessments.findById(id).orElseThrow(NotFoundException::new);
        if (a.getResidentId() != target.residentId()) {
            throw new NotFoundException();
        }
        return a;
    }

    private static void requireDraft(Assessment a) {
        if (!a.isDraft()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOT_A_DRAFT", "Lần đánh giá không còn là bản nháp, chỉ sửa bằng đính chính");
        }
    }

    private void requireAuthorOrDoctor(Assessment a) {
        CurrentUser user = currentUser.getObject();
        if (a.getPerformedBy() != user.accountId() && !user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "Chỉ người lập hoặc Bác sĩ thao tác trên bản nháp đánh giá",
                    Map.of("failedLayer", "ROLE"));
        }
    }

    /** Loại đánh giá phải hợp trạng thái người cao tuổi và đầu vào đã có hay chưa (FR-034, FR-035). */
    private void requireAssessable(long residentId, AssessmentType type) {
        ResidentStatus status = statuses.find(residentId).orElseThrow(NotFoundException::new);
        boolean initialConfirmed = assessments.existsByResidentIdAndTypeAndStatus(
                residentId, AssessmentType.INITIAL, AssessmentStatus.CONFIRMED);
        if (type == AssessmentType.INITIAL) {
            if (status != ResidentStatus.ADMISSION_IN_PROGRESS) {
                throw notAssessable();
            }
            if (initialConfirmed) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INITIAL_ASSESSMENT_EXISTS",
                        "Người cao tuổi đã có đánh giá đầu vào đã xác nhận");
            }
        } else if (!REASSESSABLE.contains(status)
                && !(status == ResidentStatus.ADMISSION_IN_PROGRESS && initialConfirmed)) {
            throw notAssessable();
        }
    }

    private static BusinessException notAssessable() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "STATUS_NOT_ASSESSABLE",
                "Trạng thái hiện tại của người cao tuổi không cho phép lập loại đánh giá này");
    }

    /** Thiếu nhận xét hoặc mức thì không xác nhận được; trả từng trường thiếu (FR-028, DBR-05). */
    private static void requireComplete(Assessment a) {
        List<UnmetCondition> unmet = new ArrayList<>();
        if (a.getComment() == null || a.getComment().isBlank()) {
            unmet.add(new UnmetCondition("comment", "Thiếu nhận xét đánh giá (DBR-05)"));
        }
        if (a.getCareLevel() == null) {
            unmet.add(new UnmetCondition("careLevel", "Thiếu mức chăm sóc (DBR-05)"));
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
    }

    private DraftContent normalized(Assessment a, DraftContent in) {
        if (in == null) {
            throw new InvalidValueException("Thiếu nội dung lần đánh giá");
        }
        String careNote = blankToNull(in.careNote());
        if (careNote != null && careNote.length() > MAX_CARE_NOTE_LENGTH) {
            throw new InvalidValueException("Lưu ý chăm sóc tối đa " + MAX_CARE_NOTE_LENGTH + " ký tự");
        }
        if (in.proposedResidenceType() != null && a.getType() != AssessmentType.INITIAL) {
            throw new InvalidValueException("Loại hình lưu trú đề xuất chỉ có ở đánh giá đầu vào");
        }
        Set<Long> needIds = in.careNeedIds() == null ? Set.of() : in.careNeedIds();
        List<CareNeed> found = careNeeds.findAllById(needIds);
        if (found.size() != needIds.size() || !found.stream().allMatch(CareNeed::isActive)) {
            throw new InvalidValueException("Nhu cầu chăm sóc không có trong danh mục hoặc đã ngừng hiệu lực");
        }
        return new DraftContent(
                normalizedBasis(in.basis()),
                blankToNull(in.comment()),
                careNote,
                in.careLevel(),
                needIds,
                in.proposedResidenceType());
    }

    private static String normalizedBasis(String basis) {
        String value = blankToNull(basis);
        if (value != null && value.length() > MAX_BASIS_LENGTH) {
            throw new InvalidValueException("Lý do, căn cứ tối đa " + MAX_BASIS_LENGTH + " ký tự");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    List<AssessmentView> views(List<Assessment> list) {
        Map<Long, Effective> effective = contents.of(list);
        Set<Long> needIds =
                list.stream().flatMap(a -> a.getCareNeedIds().stream()).collect(Collectors.toSet());
        Map<Long, String> needNames =
                careNeeds.findAllById(needIds).stream().collect(Collectors.toMap(CareNeed::getId, CareNeed::getName));
        return list.stream()
                .map(a -> {
                    Effective e = effective.get(a.getId());
                    return new AssessmentView(
                            a.getId(),
                            a.getResidentId(),
                            a.getType(),
                            a.getBasis(),
                            names.of(a.getPerformedBy()),
                            e.comment(),
                            e.careNote(),
                            e.careLevel(),
                            new TreeSet<>(a.getCareNeedIds())
                                    .stream()
                                            .map(nid -> new CareNeedView(nid, needNames.get(nid)))
                                            .sorted(Comparator.comparingLong(CareNeedView::id))
                                            .toList(),
                            a.getProposedResidenceType(),
                            a.getStatus(),
                            a.getConfirmedBy() == null ? null : names.of(a.getConfirmedBy()),
                            a.getConfirmedAt(),
                            e.corrected(),
                            a.getVersion());
                })
                .toList();
    }

    private static Map<String, Object> snapshot(Assessment a) {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "basis", a.getBasis());
        put(values, "comment", a.getComment());
        put(values, "careNote", a.getCareNote());
        put(
                values,
                "careLevel",
                a.getCareLevel() == null ? null : a.getCareLevel().name());
        if (!a.getCareNeedIds().isEmpty()) {
            values.put("careNeedIds", new TreeSet<>(a.getCareNeedIds()));
        }
        put(
                values,
                "proposedResidenceType",
                a.getProposedResidenceType() == null
                        ? null
                        : a.getProposedResidenceType().name());
        return values;
    }

    private static void put(Map<String, Object> values, String field, String value) {
        if (value != null) {
            values.put(field, value);
        }
    }
}
