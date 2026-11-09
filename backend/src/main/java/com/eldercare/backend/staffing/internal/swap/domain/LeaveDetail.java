package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.LocalDate;
import java.util.List;

/**
 * Chi tiết yêu cầu nghỉ đột xuất ở {@code simple_record} loại {@code M09.LEAVE_DETAIL} (data-model mục 5:
 * {@code yeu_cau_nghi} và {@code yeu_cau_nghi_muc} gộp, mục là danh sách trong nội dung).
 */
public record LeaveDetail(
        long requestId,
        LeaveKind kind,
        long staffId,
        String scopeType,
        Long scopeId,
        long filedByAccountId,
        boolean onBehalf,
        String reason,
        List<Item> items,
        Long approverAccountId,
        boolean selfApproved,
        java.time.Instant remindedAt,
        java.time.Instant decidedAt) {

    public static final SharedKind<LeaveDetail> KIND = SharedKind.of("M09.LEAVE_DETAIL", LeaveDetail.class);

    public enum LeaveKind {
        BY_SHIFT,
        BY_DAY
    }

    /** {@code SPLIT_OFF}: tách khi ca bắt đầu mà chưa quyết định; {@code LEFT}: ca không còn thỏa nên rời yêu cầu. */
    public enum Result {
        PENDING,
        APPROVED,
        REJECTED,
        SPLIT_OFF,
        LEFT
    }

    public record Item(Long shiftId, LocalDate date, Result result, String rejectReason, Long newLeadId, String note) {
        public Item with(Result r, String rejection, Long lead, String text) {
            return new Item(shiftId, date, r, rejection, lead, text);
        }
    }

    public static String uniqueKey(long requestId) {
        return "M09.LEAVE:" + requestId;
    }

    public LeaveDetail withItems(List<Item> newItems, Long approver, boolean self) {
        return new LeaveDetail(
                requestId,
                kind,
                staffId,
                scopeType,
                scopeId,
                filedByAccountId,
                onBehalf,
                reason,
                List.copyOf(newItems),
                approver == null ? approverAccountId : approver,
                self || selfApproved,
                remindedAt,
                decidedAt);
    }

    public LeaveDetail withKind(LeaveKind newKind, List<Item> newItems) {
        return new LeaveDetail(
                requestId,
                newKind,
                staffId,
                scopeType,
                scopeId,
                filedByAccountId,
                onBehalf,
                reason,
                List.copyOf(newItems),
                approverAccountId,
                selfApproved,
                remindedAt,
                decidedAt);
    }

    public LeaveDetail reminded(java.time.Instant at) {
        return new LeaveDetail(
                requestId,
                kind,
                staffId,
                scopeType,
                scopeId,
                filedByAccountId,
                onBehalf,
                reason,
                items,
                approverAccountId,
                selfApproved,
                at,
                decidedAt);
    }

    public LeaveDetail closed(java.time.Instant at) {
        return new LeaveDetail(
                requestId,
                kind,
                staffId,
                scopeType,
                scopeId,
                filedByAccountId,
                onBehalf,
                reason,
                items,
                approverAccountId,
                selfApproved,
                remindedAt,
                at);
    }
}
