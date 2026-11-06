package com.eldercare.backend.family.internal.feedback.domain;

import java.time.Instant;

/**
 * Phản hồi ({@code phan_hoi}, nhóm 2 cho trạng thái; data-model mục 9; FR-058 → FR-066). Nội dung, trả lời, hướng xử lý,
 * ghi chú là mục {@link FeedbackItem} (nhóm 3, R-15: {@code record_event}). Người phụ trách là một vai trò (nhóm, khi
 * {@code assigneeAccountId} trống) hoặc một tài khoản (FR-059, FR-060). Người gửi là người thân ({@code familyMemberId})
 * hoặc chính người cao tuổi (US9, FR-058a); {@code recordedOnBehalfBy} là nhân viên ghi thay khi người cao tuổi nói trực
 * tiếp; {@code wishTopic} chỉ với loại Mong muốn (FR-058b).
 */
public record Feedback(
        long id,
        long residentId,
        SenderType senderType,
        Long familyMemberId,
        Long recordedOnBehalfBy,
        Type type,
        ContentGroup group,
        WishTopic wishTopic,
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

    public enum SenderType {
        FAMILY_MEMBER,
        RESIDENT
    }

    public enum Type {
        SUGGESTION,
        COMPLAINT,
        QUESTION,
        WISH
    }

    public enum ContentGroup {
        CARE,
        HEALTH_MEDICATION,
        EATING,
        COSTS_CONTRACT,
        OTHER
    }

    /** Chủ đề của loại Mong muốn (FR-058b): khách thăm, người đón / khác. */
    public enum WishTopic {
        VISITORS_PICKERS,
        OTHER
    }

    /**
     * Cổng của người thân; ghi tại quầy (Hành chính ghi thay người thân); kênh người cao tuổi; ghi trực tiếp (nhân viên ghi
     * thay người cao tuổi, FR-058a).
     */
    public enum Source {
        PORTAL,
        COUNTER,
        RESIDENT_CHANNEL,
        RECORDED_DIRECTLY
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

    /** Người gửi; tự đóng; nhân viên ghi thay người cao tuổi (FR-058a (c)). */
    public enum CloseMethod {
        SENDER,
        AUTO,
        RECORDED_ON_BEHALF
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

    public boolean fromResident() {
        return senderType == SenderType.RESIDENT;
    }

    /** Người gửi và cách gửi khi lập phản hồi. */
    public record Sender(SenderType type, Long familyMemberId, Long recordedOnBehalfBy, Source source) {}

    /** Phản hồi mới ở Mới (FR-066 dòng Gửi). */
    public static Feedback submitted(
            long residentId,
            Sender sender,
            Type type,
            ContentGroup group,
            WishTopic wishTopic,
            Instant at,
            Priority priority,
            Instant dueAt,
            AssigneeRole assigneeRole,
            Long assigneeAccountId,
            long recordedBy) {
        return new Feedback(
                0,
                residentId,
                sender.type(),
                sender.familyMemberId(),
                sender.recordedOnBehalfBy(),
                type,
                group,
                wishTopic,
                sender.source(),
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
        return state(type, wishTopic, priority, dueBasisAt, dueAt, assigneeRole, assigneeAccountId, to, overdue)
                .marks(escalatedDueAt, repliedAt, closedAt, closeMethod);
    }

    public Feedback withAssignee(AssigneeRole role, Long accountId) {
        return state(type, wishTopic, priority, dueBasisAt, dueAt, role, accountId, status, overdue)
                .marks(escalatedDueAt, repliedAt, closedAt, closeMethod);
    }

    /** Hạn mới (đổi mức, Mở lại): dấu quá hạn tính lại cho hạn mới (FR-061 → FR-063). */
    public Feedback withDue(Priority newPriority, Instant newBasis, Instant newDue) {
        return state(type, wishTopic, newPriority, newBasis, newDue, assigneeRole, assigneeAccountId, status, false)
                .marks(escalatedDueAt, repliedAt, closedAt, closeMethod);
    }

    /** Đổi loại Mong muốn sang Góp ý, Hỏi đáp (FR-058b, FR-066): bỏ chủ đề; mức, hạn giữ nguyên. */
    public Feedback withType(Type newType) {
        return state(newType, null, priority, dueBasisAt, dueAt, assigneeRole, assigneeAccountId, status, overdue)
                .marks(escalatedDueAt, repliedAt, closedAt, closeMethod);
    }

    /** Gắn dấu quá hạn, nhớ hạn đã leo thang (FR-063). */
    public Feedback escalated() {
        return state(type, wishTopic, priority, dueBasisAt, dueAt, assigneeRole, assigneeAccountId, status, true)
                .marks(dueAt, repliedAt, closedAt, closeMethod);
    }

    public Feedback replied(Instant at) {
        return withStatus(Status.REPLIED).marks(escalatedDueAt, at, null, null);
    }

    public Feedback closed(CloseMethod method, Instant at) {
        return withStatus(Status.CLOSED).marks(escalatedDueAt, repliedAt, at, method);
    }

    private Feedback state(
            Type newType,
            WishTopic newTopic,
            Priority newPriority,
            Instant newBasis,
            Instant newDue,
            AssigneeRole role,
            Long accountId,
            Status newStatus,
            boolean newOverdue) {
        return new Feedback(
                id,
                residentId,
                senderType,
                familyMemberId,
                recordedOnBehalfBy,
                newType,
                group,
                newTopic,
                source,
                submittedAt,
                newPriority,
                newBasis,
                newDue,
                role,
                accountId,
                newStatus,
                newOverdue,
                escalatedDueAt,
                repliedAt,
                closedAt,
                closeMethod,
                recordedBy,
                version);
    }

    private Feedback marks(Instant newEscalatedDueAt, Instant newRepliedAt, Instant newClosedAt, CloseMethod method) {
        return new Feedback(
                id,
                residentId,
                senderType,
                familyMemberId,
                recordedOnBehalfBy,
                type,
                group,
                wishTopic,
                source,
                submittedAt,
                priority,
                dueBasisAt,
                dueAt,
                assigneeRole,
                assigneeAccountId,
                status,
                overdue,
                newEscalatedDueAt,
                newRepliedAt,
                newClosedAt,
                method,
                recordedBy,
                version);
    }
}
