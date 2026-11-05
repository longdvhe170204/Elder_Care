package com.eldercare.backend.family.internal.feedback.domain;

import java.time.Instant;

/**
 * Phản hồi ({@code phan_hoi}, nhóm 2 cho trạng thái; data-model mục 9; FR-058 → FR-066). Nội dung, trả lời, hướng xử lý,
 * ghi chú là mục {@link FeedbackItem} (nhóm 3, R-15: {@code record_event}). Người phụ trách là một vai trò (nhóm, khi
 * {@code assigneeAccountId} trống) hoặc một tài khoản (FR-059, FR-060).
 */
public record Feedback(
        long id,
        long residentId,
        long familyMemberId,
        Type type,
        ContentGroup group,
        Source source,
        Instant submittedAt,
        Priority priority,
        Instant dueBasisAt,
        Instant dueAt,
        AssigneeRole assigneeRole,
        Long assigneeAccountId,
        Status status,
        boolean overdue,
        Instant escalatedDueAt,
        Instant repliedAt,
        Instant closedAt,
        CloseMethod closeMethod,
        long recordedBy,
        int version) {

    public static final String ENTITY_TYPE = "M10.FEEDBACK";

    public enum Type {
        SUGGESTION,
        COMPLAINT,
        QUESTION
    }

    public enum ContentGroup {
        CARE,
        HEALTH_MEDICATION,
        EATING,
        COSTS_CONTRACT,
        OTHER
    }

    /** Cổng của người thân; ghi tại quầy (Hành chính ghi thay). */
    public enum Source {
        PORTAL,
        COUNTER
    }

    public enum Priority {
        HIGH,
        NORMAL
    }

    public enum Status {
        NEW,
        IN_PROGRESS,
        REPLIED,
        REOPENED,
        CLOSED
    }

    public enum CloseMethod {
        SENDER,
        AUTO
    }

    /** Vai trò người phụ trách; tên trùng {@code common.Role}. */
    public enum AssigneeRole {
        FLOOR_MANAGER,
        NURSING_STAFF,
        OFFICE_STAFF,
        NURSING_HOME_MANAGER
    }

    /** Chưa Đã phản hồi: còn tính hạn, còn leo thang (FR-063). */
    public boolean open() {
        return status == Status.NEW || status == Status.IN_PROGRESS || status == Status.REOPENED;
    }

    /** Phản hồi mới ở Mới (FR-066 dòng Gửi). */
    public static Feedback submitted(
            long residentId,
            long familyMemberId,
            Type type,
            ContentGroup group,
            Source source,
            Instant at,
            Priority priority,
            Instant dueAt,
            AssigneeRole assigneeRole,
            Long assigneeAccountId,
            long recordedBy) {
        return new Feedback(
                0,
                residentId,
                familyMemberId,
                type,
                group,
                source,
                at,
                priority,
                at,
                dueAt,
                assigneeRole,
                assigneeAccountId,
                Status.NEW,
                false,
                null,
                null,
                null,
                null,
                recordedBy,
                0);
    }

    public Feedback withStatus(Status to) {
        return copy(priority, dueBasisAt, dueAt, assigneeRole, assigneeAccountId, to, overdue, escalatedDueAt);
    }

    public Feedback withAssignee(AssigneeRole role, Long accountId) {
        return copy(priority, dueBasisAt, dueAt, role, accountId, status, overdue, escalatedDueAt);
    }

    /** Hạn mới (đổi mức, Mở lại): dấu quá hạn tính lại cho hạn mới (FR-061 → FR-063). */
    public Feedback withDue(Priority newPriority, Instant newBasis, Instant newDue) {
        return copy(newPriority, newBasis, newDue, assigneeRole, assigneeAccountId, status, false, escalatedDueAt);
    }

    /** Gắn dấu quá hạn, nhớ hạn đã leo thang (FR-063). */
    public Feedback escalated() {
        return copy(priority, dueBasisAt, dueAt, assigneeRole, assigneeAccountId, status, true, dueAt);
    }

    public Feedback replied(Instant at) {
        Feedback f = withStatus(Status.REPLIED);
        return new Feedback(
                f.id,
                f.residentId,
                f.familyMemberId,
                f.type,
                f.group,
                f.source,
                f.submittedAt,
                f.priority,
                f.dueBasisAt,
                f.dueAt,
                f.assigneeRole,
                f.assigneeAccountId,
                f.status,
                f.overdue,
                f.escalatedDueAt,
                at,
                null,
                null,
                f.recordedBy,
                f.version);
    }

    public Feedback closed(CloseMethod method, Instant at) {
        return new Feedback(
                id,
                residentId,
                familyMemberId,
                type,
                group,
                source,
                submittedAt,
                priority,
                dueBasisAt,
                dueAt,
                assigneeRole,
                assigneeAccountId,
                Status.CLOSED,
                overdue,
                escalatedDueAt,
                repliedAt,
                at,
                method,
                recordedBy,
                version);
    }

    private Feedback copy(
            Priority newPriority,
            Instant newBasis,
            Instant newDue,
            AssigneeRole role,
            Long accountId,
            Status newStatus,
            boolean newOverdue,
            Instant newEscalatedDueAt) {
        return new Feedback(
                id,
                residentId,
                familyMemberId,
                type,
                group,
                source,
                submittedAt,
                newPriority,
                newBasis,
                newDue,
                role,
                accountId,
                newStatus,
                newOverdue,
                newEscalatedDueAt,
                repliedAt,
                closedAt,
                closeMethod,
                recordedBy,
                version);
    }
}
