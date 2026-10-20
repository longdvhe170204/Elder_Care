package com.cambae.backend.m15.account.offline.internal;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.AlreadyProcessedException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.offline.SyncOutcome;
import com.cambae.backend.common.offline.SyncResult;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.OfflineHandler;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản ghi chờ xem lại (002 T057; data-model mục 5; FR-041c, CHK056): người quyết định là Trưởng tầng được giao của
 * tầng, không có thì Người phụ trách ca đang diễn ra, bản ghi không gắn tầng thì Quản lý viện
 * ({@code ScopeApi.reviewDecider}, tính tại lúc hỏi). Người khác không thấy bản ghi (404, FR-043). Quyết định đầu tiên
 * thắng: khóa dòng, đã quyết định thì 409 {@code ALREADY_PROCESSED}, sau đó mới so {@code If-Match}. Chấp nhận gọi
 * {@code OfflineHandler.apply} với hiệu lực từ lúc chấp nhận (giữ thời điểm thiết bị trên dòng để tham khảo); từ chối
 * bắt buộc lý do, bản ghi được lưu mà không có hiệu lực.
 */
@Service
public class PendingReviewService {

    private static final int PAGE_SIZE = 50;

    private final PendingReviewRecords records;
    private final OfflineHandlers handlers;
    private final ScopeApi scope;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final BusinessTime time;

    PendingReviewService(
            PendingReviewRecords records,
            OfflineHandlers handlers,
            ScopeApi scope,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            BusinessTime time) {
        this.records = records;
        this.handlers = handlers;
        this.scope = scope;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.time = time;
    }

    public record Page(List<PendingReviewRecord> items, String nextCursor) {}

    /** Bản ghi đang chờ mà người dùng là người quyết định, theo id tăng dần; con trỏ là id cuối của trang trước. */
    public Page page(String cursor) {
        CurrentUser user = currentUser.getObject();
        long after = cursor == null || cursor.isBlank() ? 0 : Long.parseLong(cursor);
        List<PendingReviewRecord> mine = records.pendingAfter(after).stream()
                .filter(r -> decides(user, r))
                .limit(PAGE_SIZE + 1L)
                .toList();
        if (mine.size() <= PAGE_SIZE) {
            return new Page(mine, null);
        }
        List<PendingReviewRecord> items = mine.subList(0, PAGE_SIZE);
        return new Page(items, Long.toString(items.getLast().id()));
    }

    @BusinessCommand(requiresReason = false)
    @Transactional
    public PendingReviewRecord accept(long id, String ifMatch) {
        CurrentUser user = currentUser.getObject();
        PendingReviewRecord record = decidable(user, id, ifMatch);
        OfflineHandler<Object> handler = handlers.byType(record.recordType())
                .orElseThrow(() -> new IllegalStateException("Không có OfflineHandler cho " + record.recordType()));
        Instant now = time.now();
        String reference = handler.apply(
                handlers.content(handler, record.content()), new SyncOutcome(SyncResult.ACCEPTED, null), now);
        records.decide(id, ReviewStatus.ACCEPTED, user.accountId(), now, null, reference);
        audit(OfflineAuditActions.OFFLINE_RECORD_ACCEPTED, record, ReviewStatus.ACCEPTED, null, reference);
        return records.find(id).orElseThrow();
    }

    @BusinessCommand
    @Transactional
    public PendingReviewRecord reject(long id, String ifMatch, Reason reason) {
        CurrentUser user = currentUser.getObject();
        PendingReviewRecord record = decidable(user, id, ifMatch);
        records.decide(id, ReviewStatus.REJECTED, user.accountId(), time.now(), reason.value(), null);
        audit(OfflineAuditActions.OFFLINE_RECORD_REJECTED, record, ReviewStatus.REJECTED, reason, null);
        return records.find(id).orElseThrow();
    }

    private PendingReviewRecord decidable(CurrentUser user, long id, String ifMatch) {
        PendingReviewRecord record = records.lock(id).orElseThrow(NotFoundException::new);
        if (!decides(user, record)) {
            throw new NotFoundException();
        }
        if (record.status() != ReviewStatus.PENDING_REVIEW) {
            throw new AlreadyProcessedException("Bản ghi đã được quyết định");
        }
        VersionEtag.requireMatch(ifMatch, record.version(), record);
        return record;
    }

    private boolean decides(CurrentUser user, PendingReviewRecord record) {
        return scope.reviewDecider(Optional.ofNullable(record.floorAreaId()))
                .accountIds()
                .contains(user.accountId());
    }

    private void audit(
            AuditAction action, PendingReviewRecord record, ReviewStatus to, Reason reason, String reference) {
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(action)
                .entity(OfflineAuditActions.ENTITY, record.id())
                .before(Map.of("status", ReviewStatus.PENDING_REVIEW.name()))
                .after(
                        reference == null
                                ? Map.of("status", to.name())
                                : Map.of("status", to.name(), "reference", reference))
                .details(Map.of("recordType", record.recordType(), "recorderId", record.recorderId()))
                .deviceTime(record.deviceTime());
        if (reason != null) {
            entry.reason(reason);
        }
        auditLog.write(entry.build());
    }
}
