package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.care.internal.careplan.domain.CarePlanReviewRaised;
import com.eldercare.backend.care.internal.careplan.domain.CarePlanReviewRule;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanReviewStore;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu xem xét kế hoạch chăm sóc (FR-058, Q-448): đọc yêu cầu Đang mở theo {@link CarePlanReviewRule}, lệnh "Không cần
 * thay đổi" đóng mọi yêu cầu Đang mở của người cao tuổi.
 */
@Service
public class CarePlanReviews {
    private static final PermissionKey VIEW = new PermissionKey(Function.CARE_PLAN, Operation.VIEW);
    private static final PermissionKey CREATE = new PermissionKey(Function.CARE_PLAN, Operation.CREATE);
    private static final PermissionKey APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);

    private final CarePlanReviewStore reviews;
    private final CarePlanStore plans;
    private final CareLockStore locks;
    private final ResidentApi residents;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public CarePlanReviews(
            CarePlanReviewStore reviews,
            CarePlanStore plans,
            CareLockStore locks,
            ResidentApi residents,
            CurrentUser user,
            PermissionApi permissions,
            AuditLogApi audit,
            BusinessTime time) {
        this.reviews = reviews;
        this.plans = plans;
        this.locks = locks;
        this.residents = residents;
        this.user = user;
        this.permissions = permissions;
        this.audit = audit;
        this.time = time;
    }

    /** Yêu cầu Đang mở của người cao tuổi, cũ trước. */
    public List<CarePlanReviewRaised> open(long residentId) {
        return CarePlanReviewRule.open(
                reviews.raised(residentId), plans.list(residentId, false), reviews.dismissals(residentId));
    }

    /** Yêu cầu Đang mở của một người cao tuổi, cho màn kế hoạch (FR-059). */
    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.VIEW)
    public List<CarePlanReviewRaised> open(Target target, long residentId) {
        return open(residentId);
    }

    /**
     * Một dòng hàng chờ: yêu cầu Đang mở, bản đang xử lý (Nháp bị trả lại hay đang soạn, hoặc Chờ duyệt), thời điểm bắt
     * đầu chờ (bản Chờ duyệt thì là lúc gửi).
     */
    public record QueueRow(
            long residentId, List<CarePlanReviewRaised> reasons, Optional<CarePlan> draft, Instant waitingSince) {}

    /**
     * Hàng chờ "Cần lập hoặc sửa kế hoạch" (FR-059): người chưa ở trạng thái cuối, trong phạm vi xem kế hoạch, còn yêu cầu
     * Đang mở hoặc có bản Nháp; chờ lâu nhất trước.
     */
    @Transactional(readOnly = true)
    public List<QueueRow> queue() {
        var candidates = new TreeSet<Long>(reviews.residentsRaised());
        candidates.addAll(plans.residentsWithDraft());
        var rows = new ArrayList<QueueRow>();
        for (long residentId : candidates) {
            if (residents.isFinalStatus(residentId)) continue;
            if (!permissions
                    .hasPermission(user, VIEW, CarePlanService.residentTarget(residentId, 0))
                    .allowed()) continue;
            var all = plans.list(residentId, false);
            var open = CarePlanReviewRule.open(reviews.raised(residentId), all, reviews.dismissals(residentId));
            var draft = all.stream()
                    .filter(p -> p.status() == CarePlanStatus.DRAFT || p.status() == CarePlanStatus.PENDING_APPROVAL)
                    .findFirst();
            if (open.isEmpty() && draft.isEmpty()) continue;
            // Q-448 (sửa 2026-11-26): bản Chờ duyệt hiện ở nhóm "Đang chờ Bác sĩ duyệt", tính từ lúc gửi
            boolean pending = draft.map(p -> p.status() == CarePlanStatus.PENDING_APPROVAL)
                    .orElse(false);
            Instant since = pending
                    ? draft.get().submittedAt()
                    : open.isEmpty() ? draft.get().createdAt() : open.getFirst().raisedAt();
            rows.add(new QueueRow(residentId, open, draft, since));
        }
        rows.sort(Comparator.comparing(QueueRow::waitingSince));
        return rows;
    }

    /** "Không cần thay đổi": người có quyền Lập hoặc Duyệt kế hoạch chăm sóc trong phạm vi, lý do bắt buộc. */
    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.VIEW)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void dismiss(Target target, long residentId, Reason reason) {
        if (!permissions.hasPermission(user, CREATE, target).allowed()
                && !permissions.hasPermission(user, APPROVE, target).allowed())
            throw new PermissionDeniedException("Cần quyền Lập hoặc Duyệt kế hoạch chăm sóc");
        if (reason == null) throw new ReasonRequiredException();
        locks.lock(List.of(residentId));
        if (residents.isFinalStatus(residentId))
            throw new BusinessException(HttpStatus.CONFLICT, "RESIDENT_FINAL_STATUS", "Hồ sơ đã ở trạng thái cuối");
        var open = open(residentId);
        if (open.isEmpty())
            throw new BusinessException(HttpStatus.CONFLICT, "NO_OPEN_REVIEW", "Không có yêu cầu xem xét nào đang mở");
        reviews.dismiss(residentId, user.accountId(), reason.value(), time.now());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_PLAN_REVIEW_DISMISSED", true))
                .entity("M01.RESIDENT", residentId)
                .before(Map.of(
                        "openReviews",
                        open.stream().map(r -> r.source() + ":" + r.reference()).toList()))
                .after(Map.of("openReviews", List.of()))
                .reason(reason)
                .build());
    }
}
