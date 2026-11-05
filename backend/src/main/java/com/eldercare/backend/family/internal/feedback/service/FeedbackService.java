package com.eldercare.backend.family.internal.feedback.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.events.FeedbackNotice;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.AssigneeRole;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.CloseMethod;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Source;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
import com.eldercare.backend.family.internal.feedback.domain.FeedbackDeadline;
import com.eldercare.backend.family.internal.feedback.domain.FeedbackItem;
import com.eldercare.backend.family.internal.feedback.persistence.FeedbackStore;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phản hồi, khiếu nại (012 T030; US6; FR-058 → FR-067, BR-M10-05; research F-13). Người thân gửi qua cổng cho chính
 * mình, Hành chính ghi thay tại quầy. Người phụ trách theo nhóm nội dung (FR-059): Trưởng tầng được giao của tầng người
 * cao tuổi lúc gửi, tầng không có thì Quản lý viện; chi phí, hợp đồng, khác là nhóm Hành chính tới khi một người Nhận xử
 * lý. Lệnh theo bảng FR-066; nội dung, trả lời là mục không sửa (FR-065).
 */
@Service
public class FeedbackService {

    static final String RESIDENT_ENTITY = "M01.RESIDENT";
    private static final AuditAction SUBMITTED = new AuditAction("FEEDBACK_SUBMITTED", false);
    private static final AuditAction CHANGED = new AuditAction("FEEDBACK_CHANGED", false);
    private static final Set<Role> REASSIGNERS =
            Set.of(Role.FLOOR_MANAGER, Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);
    /** Người nhận giao lại: có quyền T ở dòng "Phản hồi" (FR-060), theo thứ tự ưu tiên vai trò. */
    private static final List<Role> ASSIGNABLE = List.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.OFFICE_STAFF);
    /** Mục người gửi thấy (contracts: bản cho người gửi chỉ phần trả lời họ được thấy). */
    private static final Set<FeedbackItem.Type> SENDER_ITEMS = Set.of(
            FeedbackItem.Type.CONTENT,
            FeedbackItem.Type.REPLY,
            FeedbackItem.Type.HANDLING_DIRECTION,
            FeedbackItem.Type.REOPEN);

    /** Thân gửi ({@code SubmitFeedback}, phần US6). */
    public record Submission(
            long residentId, Long familyMemberId, Type type, ContentGroup group, String content, List<Long> fileIds) {}

    /** Thân lệnh FR-066. */
    public record CommandBody(
            Reason reason,
            String handlingDirection,
            String content,
            Long assigneeId,
            Priority priority,
            List<Long> fileIds) {}

    /** Một mục cho người xem: loại, nội dung, người ghi, thời điểm. */
    public record ItemView(long id, FeedbackItem.Type type, String content, List<Long> fileIds, Long by, Instant at) {}

    /**
     * Phản hồi cho người xem. Nhân viên: {@code senderHealthRightEffective} để cảnh báo lúc soạn trả lời (FR-058). Người
     * gửi: {@code healthReplyHidden} khi có mục sức khỏe bị ẩn vì quyền xem sức khỏe không có tác dụng.
     */
    public record FeedbackView(
            Feedback feedback,
            String content,
            List<Long> fileIds,
            String assignee,
            boolean overdue,
            List<ItemView> items,
            Boolean senderHealthRightEffective,
            Boolean healthReplyHidden,
            String familyMemberName,
            List<String> allowedCommands) {}

    /**
     * Người nhận giao lại được (FR-060): tài khoản, họ tên ({@code null} khi chưa có hồ sơ nhân viên, như
     * {@code AccountApi.fullName}), vai trò Trưởng tầng, Điều dưỡng hoặc Hành chính.
     */
    public record AssignableView(long accountId, String fullName, Role role) {}

    private final FeedbackStore store;
    private final RecordEventApi recordEvents;
    private final FeedbackRules rules;
    private final RelationshipQueries relationships;
    private final FamilyRightsApi rights;
    private final ResidentLocationApi locations;
    private final ScopeApi scopes;
    private final AccountApi accounts;
    private final ApproverApi approvers;
    private final PermissionApi permissions;
    private final AuditLogApi auditLog;
    private final FileApi files;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ApplicationEventPublisher events;

    FeedbackService(
            FeedbackStore store,
            RecordEventApi recordEvents,
            FeedbackRules rules,
            RelationshipQueries relationships,
            FamilyRightsApi rights,
            ResidentLocationApi locations,
            ScopeApi scopes,
            AccountApi accounts,
            ApproverApi approvers,
            PermissionApi permissions,
            AuditLogApi auditLog,
            FileApi files,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ApplicationEventPublisher events) {
        this.store = store;
        this.recordEvents = recordEvents;
        this.rules = rules;
        this.relationships = relationships;
        this.rights = rights;
        this.locations = locations;
        this.scopes = scopes;
        this.accounts = accounts;
        this.approvers = approvers;
        this.permissions = permissions;
        this.auditLog = auditLog;
        this.files = files;
        this.time = time;
        this.currentUser = currentUser;
        this.events = events;
    }

    /** Gửi (FR-058, FR-059, FR-061, FR-062): Mới, đặt người phụ trách, mức, hạn; báo người phụ trách. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long submit(Submission submission) {
        CurrentUser user = currentUser.getObject();
        long residentId = submission.residentId();
        authorize(user, Operation.CREATE, residentTarget(residentId));
        long familyMemberId;
        Source source;
        if (user.type() == AccountType.FAMILY_MEMBER) {
            familyMemberId = user.familyMemberId().orElseThrow();
            if (submission.familyMemberId() != null && submission.familyMemberId() != familyMemberId) {
                throw denied();
            }
            source = Source.PORTAL;
        } else if (user.type() == AccountType.STAFF && user.roles().contains(Role.OFFICE_STAFF)) {
            if (submission.familyMemberId() == null) {
                throw new InvalidValueException("Chọn người thân gửi phản hồi (familyMemberId)");
            }
            familyMemberId = submission.familyMemberId();
            source = Source.COUNTER;
        } else {
            throw denied();
        }
        if (relationships.active(familyMemberId, residentId).isEmpty()) {
            throw new InvalidValueException("Người thân không có quan hệ Hiệu lực với người cao tuổi");
        }
        if (submission.type() == null || submission.group() == null || blank(submission.content())) {
            throw new InvalidValueException("Chọn loại, nhóm nội dung và ghi nội dung phản hồi");
        }
        Instant now = time.now();
        AssigneeRole role = FeedbackDeadline.defaultAssignee(submission.group());
        Long assigneeAccount = null;
        if (role == AssigneeRole.FLOOR_MANAGER) {
            assigneeAccount = locations
                    .at(residentId, now)
                    .map(LocationView::floorId)
                    .flatMap(scopes::assignedFloorManager)
                    .orElse(null);
            if (assigneeAccount == null) {
                role = AssigneeRole.NURSING_HOME_MANAGER;
            }
        }
        Priority priority = FeedbackDeadline.defaultPriority(submission.type());
        Feedback draft = Feedback.submitted(
                residentId,
                familyMemberId,
                submission.type(),
                submission.group(),
                source,
                now,
                priority,
                FeedbackDeadline.dueAt(now, priority, rules.due()),
                role,
                assigneeAccount,
                user.accountId());
        long id = store.insert(draft, now);
        List<Long> fileIds = submission.fileIds() == null ? List.of() : submission.fileIds();
        RecordEvent<FeedbackItem> item = addItem(
                id,
                new FeedbackItem(FeedbackItem.Type.CONTENT, submission.content().strip(), false, fileIds),
                user,
                now);
        fileIds.forEach(f -> files.attach(f, Feedback.ENTITY_TYPE, id));
        auditLog.write(AuditLogEntry.builder()
                .action(SUBMITTED)
                .entity(Feedback.ENTITY_TYPE, id)
                .resident(residentId)
                .after(Map.of("status", Status.NEW.name(), "priority", priority.name(), "source", source.name()))
                .build());
        Feedback saved = store.require(id);
        notifyAssignee(saved, String.valueOf(item.id()), now);
        return id;
    }

    /** Danh sách: người thân chỉ phản hồi của mình; nhân viên theo phạm vi; {@code overdue} cho Quản lý viện (FR-063). */
    @Transactional(readOnly = true)
    public List<FeedbackView> list(Status status, Boolean overdue) {
        CurrentUser user = currentUser.getObject();
        List<Feedback> rows;
        if (user.type() == AccountType.FAMILY_MEMBER) {
            authorizeRole(user, Operation.VIEW);
            rows = store.ofFamilyMember(user.familyMemberId().orElseThrow());
        } else {
            // Lớp vai trò ở đây, phạm vi lọc từng phản hồi (Target.UNSPECIFIED chỉ phạm vi toàn viện đạt)
            authorizeRole(user, Operation.VIEW);
            rows = store.all().stream()
                    .filter(f -> permissions
                            .hasPermission(user, key(Operation.VIEW), target(f))
                            .allowed())
                    .toList();
        }
        Instant now = time.now();
        return rows.stream()
                .filter(f -> status == null || f.status() == status)
                .filter(f -> overdue == null || overdue == overdue(f))
                .map(f -> view(f, user, false, now))
                .toList();
    }

    /** Chi tiết; người gửi chỉ thấy phần trả lời họ được thấy (FR-058, FR-065). */
    @Transactional(readOnly = true)
    public FeedbackView get(long id) {
        CurrentUser user = currentUser.getObject();
        Feedback f = store.require(id);
        authorize(user, Operation.VIEW, target(f));
        requireOwnIfFamily(user, f);
        return view(f, user, true, time.now());
    }

    /** Nhận xử lý (Mới, Mở lại → Đang xử lý); người nhận trong nhóm vai trò thành người phụ trách. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView startHandling(long id) {
        CurrentUser user = currentUser.getObject();
        Feedback current = lockAsAssignee(user, id);
        requireStatus(current, Status.NEW, Status.REOPENED);
        Long account = current.assigneeAccountId() == null ? user.accountId() : current.assigneeAccountId();
        Feedback after = store.update(
                current.withAssignee(current.assigneeRole(), account).withStatus(Status.IN_PROGRESS));
        audit(current, after, null);
        return view(after, user, true, time.now());
    }

    /** Trả lời (Đang xử lý → Đã phản hồi): có hướng xử lý và nội dung; báo người gửi (FR-058, FR-066). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView reply(long id, CommandBody body) {
        CurrentUser user = currentUser.getObject();
        Feedback current = lockAsAssignee(user, id);
        requireStatus(current, Status.IN_PROGRESS);
        if (blank(body.handlingDirection()) || blank(body.content())) {
            throw new InvalidValueException("Ghi hướng xử lý và nội dung trả lời");
        }
        Instant now = time.now();
        boolean health = current.group() == ContentGroup.HEALTH_MEDICATION;
        List<Long> fileIds = body.fileIds() == null ? List.of() : body.fileIds();
        addItem(
                id,
                new FeedbackItem(
                        FeedbackItem.Type.HANDLING_DIRECTION,
                        body.handlingDirection().strip(),
                        health,
                        List.of()),
                user,
                now);
        RecordEvent<FeedbackItem> replyItem = addItem(
                id, new FeedbackItem(FeedbackItem.Type.REPLY, body.content().strip(), health, fileIds), user, now);
        fileIds.forEach(f -> files.attach(f, Feedback.ENTITY_TYPE, id));
        Feedback after = store.update(current.replied(now));
        audit(current, after, null);
        events.publishEvent(new FeedbackNotice(
                id,
                after.residentId(),
                FeedbackNotice.Kind.REPLIED,
                new RecipientGroup.FamilyMember(after.familyMemberId(), "FEEDBACK_SENDER"),
                health,
                String.valueOf(replyItem.id()),
                now));
        return view(after, user, true, now);
    }

    /** Ghi chú bổ sung của người phụ trách (FR-065). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView addNote(long id, CommandBody body) {
        CurrentUser user = currentUser.getObject();
        Feedback current = lockAsAssignee(user, id);
        if (blank(body.content())) {
            throw new InvalidValueException("Ghi nội dung ghi chú");
        }
        Instant now = time.now();
        addItem(id, new FeedbackItem(FeedbackItem.Type.NOTE, body.content().strip(), false, List.of()), user, now);
        return view(current, user, true, now);
    }

    /** Giao lại (FR-060, FR-063): Trưởng tầng, Hành chính, Quản lý viện; lý do bắt buộc; báo người được giao. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView reassign(long id, CommandBody body) {
        CurrentUser user = currentUser.getObject();
        Feedback current = store.lockForUpdate(id);
        authorizeHandling(user, current);
        if (user.roles().stream().noneMatch(REASSIGNERS::contains)) {
            throw denied();
        }
        requireStatus(current, Status.NEW, Status.IN_PROGRESS, Status.REOPENED);
        if (body.reason() == null) {
            throw new ReasonRequiredException();
        }
        if (body.assigneeId() == null) {
            throw new InvalidValueException("Chọn người được giao (assigneeId)");
        }
        // Người có (FEEDBACK, UPDATE) trong phạm vi của người cao tuổi, vai trò Trưởng tầng, Điều dưỡng, Hành chính
        Role role = assignable(current).stream()
                .filter(a -> a.accountId() == body.assigneeId())
                .map(AssignableView::role)
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Người được giao không có quyền xử lý phản hồi này"));
        long assigneeAccount = body.assigneeId();
        Instant now = time.now();
        RecordEvent<FeedbackItem> item = addItem(
                id,
                new FeedbackItem(FeedbackItem.Type.REASSIGNMENT, body.reason().value(), false, List.of()),
                user,
                now);
        Feedback after = store.update(current.withAssignee(AssigneeRole.valueOf(role.name()), assigneeAccount));
        audit(current, after, body.reason());
        notifyAssignee(after, String.valueOf(item.id()), now);
        return view(after, user, true, now);
    }

    /** Đổi mức (FR-062): lý do; hạn tính lại từ mốc tính hạn; hạn mới đã qua thì quá hạn và leo thang ngay. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView changePriority(long id, CommandBody body) {
        CurrentUser user = currentUser.getObject();
        Feedback current = lockAsAssignee(user, id);
        requireStatus(current, Status.NEW, Status.IN_PROGRESS, Status.REOPENED);
        if (body.reason() == null) {
            throw new ReasonRequiredException();
        }
        if (body.priority() == null) {
            throw new InvalidValueException("Chọn mức ưu tiên mới");
        }
        Instant now = time.now();
        Instant due = FeedbackDeadline.dueAt(current.dueBasisAt(), body.priority(), rules.due());
        Feedback changed = current.withDue(body.priority(), current.dueBasisAt(), due);
        boolean escalate = FeedbackDeadline.shouldEscalate(changed.status(), due, changed.escalatedDueAt(), now);
        if (!now.isBefore(due)) {
            changed = changed.escalated();
        }
        addItem(
                id,
                new FeedbackItem(
                        FeedbackItem.Type.PRIORITY_CHANGE, body.reason().value(), false, List.of()),
                user,
                now);
        Feedback after = store.update(changed);
        audit(current, after, body.reason());
        if (escalate) {
            notifyEscalation(after, now);
        }
        return view(after, user, true, now);
    }

    /** Đồng ý đóng (Đã phản hồi → Đóng), người gửi (FR-066). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView agreeToClose(long id) {
        CurrentUser user = currentUser.getObject();
        Feedback current = lockAsSender(user, id);
        requireStatus(current, Status.REPLIED);
        Instant now = time.now();
        Feedback after = store.update(current.closed(CloseMethod.SENDER, now));
        audit(current, after, null);
        return view(after, user, true, now);
    }

    /** Mở lại (Đã phản hồi → Mở lại), người gửi kèm lý do; hạn mới từ lúc mở lại; báo người phụ trách (FR-061). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public FeedbackView reopen(long id, CommandBody body) {
        CurrentUser user = currentUser.getObject();
        Feedback current = lockAsSender(user, id);
        requireStatus(current, Status.REPLIED);
        if (body.reason() == null) {
            throw new ReasonRequiredException();
        }
        Instant now = time.now();
        RecordEvent<FeedbackItem> item = addItem(
                id, new FeedbackItem(FeedbackItem.Type.REOPEN, body.reason().value(), false, List.of()), user, now);
        Feedback after = store.update(
                current.withDue(current.priority(), now, FeedbackDeadline.dueAt(now, current.priority(), rules.due()))
                        .withStatus(Status.REOPENED));
        audit(current, after, body.reason());
        notifyAssignee(after, String.valueOf(item.id()), now);
        return view(after, user, true, now);
    }

    /** Leo thang một phản hồi quá hạn (việc theo phút, FR-063); {@code false} khi không còn cần. */
    @Transactional
    public boolean escalateIfDue(long id, Instant now) {
        Feedback current = store.lockForUpdate(id);
        if (!FeedbackDeadline.shouldEscalate(current.status(), current.dueAt(), current.escalatedDueAt(), now)) {
            return false;
        }
        Feedback after = store.update(current.escalated());
        notifyEscalation(after, now);
        return true;
    }

    /** Tự đóng phản hồi Đã phản hồi quá CFG-M10-02 (việc theo phút, FR-064). */
    @Transactional
    public boolean autoCloseIfDue(long id, Instant now) {
        Feedback current = store.lockForUpdate(id);
        if (!FeedbackDeadline.shouldAutoClose(current.status(), current.repliedAt(), rules.autoClose(), now)) {
            return false;
        }
        store.update(current.closed(CloseMethod.AUTO, now));
        return true;
    }

    /** Người xem được tệp của phản hồi: người gửi, nhân viên có quyền xem trong phạm vi. */
    @Transactional(readOnly = true)
    public boolean canView(long id) {
        CurrentUser user = currentUser.getObject();
        Optional<Feedback> f = store.find(id);
        if (f.isEmpty()
                || !permissions
                        .hasPermission(user, key(Operation.VIEW), target(f.get()))
                        .allowed()) {
            return false;
        }
        return user.type() != AccountType.FAMILY_MEMBER
                || f.get().familyMemberId() == user.familyMemberId().orElseThrow();
    }

    private FeedbackView view(Feedback f, CurrentUser user, boolean withItems, Instant now) {
        boolean family = user.type() == AccountType.FAMILY_MEMBER;
        List<RecordEvent<FeedbackItem>> rows = recordEvents.list(FeedbackItem.KIND, parent(f.id()));
        RecordEvent<FeedbackItem> first = rows.stream()
                .filter(r -> r.content().type() == FeedbackItem.Type.CONTENT)
                .findFirst()
                .orElseThrow();
        boolean healthRight = rights.rightEffective(f.familyMemberId(), f.residentId(), FamilyRight.VIEW_HEALTH, now);
        List<ItemView> items = new ArrayList<>();
        boolean hidden = false;
        if (withItems) {
            for (RecordEvent<FeedbackItem> r : rows) {
                FeedbackItem item = r.content();
                if (family && !SENDER_ITEMS.contains(item.type())) {
                    continue;
                }
                if (family && item.healthInformation() && !healthRight) {
                    hidden = true;
                    continue;
                }
                items.add(
                        new ItemView(r.id(), item.type(), item.content(), item.fileIds(), r.actorId(), r.occurredAt()));
            }
        }
        return new FeedbackView(
                f,
                first.content().content(),
                first.content().fileIds(),
                assigneeName(f),
                overdue(f),
                items,
                family ? null : healthRight,
                family ? hidden : null,
                relationships
                        .familyMember(f.familyMemberId())
                        .map(m -> m.fullName())
                        .orElse(null),
                allowedCommands(f, user));
    }

    /** Họ tên người phụ trách; chưa ai nhận hoặc tài khoản chưa có hồ sơ nhân viên thì tên vai trò của nhóm. */
    private String assigneeName(Feedback f) {
        if (f.assigneeAccountId() != null) {
            Optional<String> name = accounts.fullName(f.assigneeAccountId());
            if (name.isPresent()) {
                return name.get();
            }
        }
        return switch (f.assigneeRole()) {
            case FLOOR_MANAGER -> "Trưởng tầng";
            case NURSING_STAFF -> "Điều dưỡng";
            case OFFICE_STAFF -> "Hành chính";
            case NURSING_HOME_MANAGER -> "Quản lý viện";
        };
    }

    private static boolean overdue(Feedback f) {
        return f.overdue() && f.open();
    }

    private void notifyAssignee(Feedback f, String occurrence, Instant now) {
        RecipientGroup recipient;
        if (f.assigneeAccountId() != null) {
            Long staffId = accounts.byId(f.assigneeAccountId())
                    .map(AccountView::staffId)
                    .orElse(null);
            if (staffId == null) {
                return;
            }
            recipient = new RecipientGroup.Staff(staffId, "ASSIGNEE");
        } else {
            recipient = new RecipientGroup.FacilityRole(
                    Role.valueOf(f.assigneeRole().name()));
        }
        events.publishEvent(new FeedbackNotice(
                f.id(), f.residentId(), FeedbackNotice.Kind.ASSIGNED, recipient, false, occurrence, now));
    }

    private void notifyEscalation(Feedback f, Instant now) {
        events.publishEvent(new FeedbackNotice(
                f.id(),
                f.residentId(),
                FeedbackNotice.Kind.ESCALATED,
                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                false,
                String.valueOf(f.dueAt().toEpochMilli()),
                now));
    }

    private RecordEvent<FeedbackItem> addItem(long feedbackId, FeedbackItem item, CurrentUser user, Instant now) {
        return recordEvents.add(
                FeedbackItem.KIND,
                parent(feedbackId),
                new NewRecordEvent<>(item, null, user.accountId(), false, now, null, now, null, null));
    }

    private static EntityRef parent(long feedbackId) {
        return new EntityRef(Feedback.ENTITY_TYPE, feedbackId);
    }

    /** Khóa phản hồi, kiểm quyền xử lý và người gọi là người phụ trách (FR-066). */
    private Feedback lockAsAssignee(CurrentUser user, long id) {
        Feedback current = store.lockForUpdate(id);
        authorizeHandling(user, current);
        if (!isAssignee(user, current)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Chỉ người phụ trách xử lý phản hồi", Map.of("failedLayer", "ROLE"));
        }
        return current;
    }

    /** Người phụ trách: đúng tài khoản, hoặc người có vai trò của nhóm khi chưa ai Nhận xử lý (FR-059). */
    private static boolean isAssignee(CurrentUser user, Feedback f) {
        return f.assigneeAccountId() == null
                ? user.roles().contains(Role.valueOf(f.assigneeRole().name()))
                : f.assigneeAccountId() == user.accountId();
    }

    private static Operation handlingOperation(CurrentUser user) {
        return user.roles().contains(Role.NURSING_HOME_MANAGER) ? Operation.APPROVE : Operation.UPDATE;
    }

    /** Lệnh người xem gọi được lúc này theo bảng FR-066 (DESIGN.md 7.4); backend vẫn kiểm lại ở lệnh. */
    private List<String> allowedCommands(Feedback f, CurrentUser user) {
        List<String> commands = new ArrayList<>();
        if (user.type() == AccountType.FAMILY_MEMBER) {
            boolean own = f.familyMemberId() == user.familyMemberId().orElseThrow();
            if (own
                    && f.status() == Status.REPLIED
                    && permissions
                            .hasPermission(user, key(Operation.UPDATE), target(f))
                            .allowed()) {
                commands.add("agree-to-close");
                commands.add("reopen");
            }
            return commands;
        }
        if (user.type() != AccountType.STAFF
                || !permissions
                        .hasPermission(user, key(handlingOperation(user)), target(f))
                        .allowed()) {
            return commands;
        }
        if (isAssignee(user, f)) {
            if (f.status() == Status.NEW || f.status() == Status.REOPENED) {
                commands.add("start-handling");
            }
            if (f.status() == Status.IN_PROGRESS) {
                commands.add("reply");
            }
            if (f.open()) {
                commands.add("change-priority");
            }
            commands.add("add-note");
        }
        if (f.open() && user.roles().stream().anyMatch(REASSIGNERS::contains)) {
            commands.add("reassign");
        }
        return commands;
    }

    /** Người nhận giao lại được (FR-060): người có (FEEDBACK, UPDATE) trên phản hồi, theo vai trò được giao. */
    @Transactional(readOnly = true)
    public List<AssignableView> assignableStaff(long id) {
        CurrentUser user = currentUser.getObject();
        Feedback f = store.require(id);
        authorizeHandling(user, f);
        return assignable(f).stream()
                .sorted(java.util.Comparator.comparing(AssignableView::role)
                        .thenComparing(
                                AssignableView::fullName,
                                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    private List<AssignableView> assignable(Feedback f) {
        List<AssignableView> result = new ArrayList<>();
        for (long accountId : approvers.approvers(key(Operation.UPDATE), target(f))) {
            accounts.byId(accountId)
                    .flatMap(
                            a -> ASSIGNABLE.stream().filter(a.roles()::contains).findFirst())
                    .ifPresent(role -> result.add(new AssignableView(
                            accountId, accounts.fullName(accountId).orElse(null), role)));
        }
        return result;
    }

    /** Nhân viên: Tạo, Sửa trong phạm vi; Quản lý viện qua quyền Duyệt (Phụ lục 27 dòng "Phản hồi" ô D). */
    private void authorizeHandling(CurrentUser user, Feedback f) {
        if (user.type() != AccountType.STAFF) {
            throw denied();
        }
        authorize(user, handlingOperation(user), target(f));
    }

    private Feedback lockAsSender(CurrentUser user, long id) {
        Feedback current = store.lockForUpdate(id);
        if (user.type() != AccountType.FAMILY_MEMBER) {
            throw denied();
        }
        authorize(user, Operation.UPDATE, target(current));
        requireOwnIfFamily(user, current);
        return current;
    }

    private static void requireOwnIfFamily(CurrentUser user, Feedback f) {
        if (user.type() == AccountType.FAMILY_MEMBER
                && f.familyMemberId() != user.familyMemberId().orElseThrow()) {
            throw new NotFoundException();
        }
    }

    private static void requireStatus(Feedback f, Status... allowed) {
        for (Status s : allowed) {
            if (f.status() == s) {
                return;
            }
        }
        throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Phản hồi đã đổi trạng thái");
    }

    private void audit(Feedback before, Feedback after, Reason reason) {
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(CHANGED)
                .entity(Feedback.ENTITY_TYPE, after.id())
                .resident(after.residentId())
                .before(Map.of(
                        "status", before.status().name(),
                        "priority", before.priority().name(),
                        "assignee", String.valueOf(before.assigneeAccountId())))
                .after(Map.of(
                        "status", after.status().name(),
                        "priority", after.priority().name(),
                        "assignee", String.valueOf(after.assigneeAccountId())));
        if (reason != null) {
            entry.reason(reason);
        }
        auditLog.write(entry.build());
    }

    static Target target(Feedback f) {
        return new Target(Feedback.ENTITY_TYPE, String.valueOf(f.id()), f.residentId(), null);
    }

    private static Target residentTarget(long residentId) {
        return new Target(RESIDENT_ENTITY, String.valueOf(residentId), residentId, null);
    }

    private static PermissionKey key(Operation operation) {
        return new PermissionKey(Function.FEEDBACK, operation);
    }

    private void authorizeRole(CurrentUser user, Operation operation) {
        if (!permissions.permissions(user).contains(key(operation))) {
            throw denied();
        }
    }

    private void authorize(CurrentUser user, Operation operation, Target target) {
        PermissionResult result = permissions.hasPermission(user, key(operation), target);
        if (result.allowed()) {
            return;
        }
        FailedLayer layer = result.failedLayer().orElse(FailedLayer.ROLE);
        if (layer == FailedLayer.SCOPE && !target.equals(Target.UNSPECIFIED)) {
            throw new NotFoundException();
        }
        throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", layer.name()));
    }

    private static PermissionDeniedException denied() {
        return new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
