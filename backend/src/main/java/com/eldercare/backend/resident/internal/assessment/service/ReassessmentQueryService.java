package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.dto.ReassessmentState;
import com.eldercare.backend.resident.internal.assessment.service.ReassessmentNoticeService.Detail;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Truy vấn "cần đánh giá lại" qua HTTP (001 T066; FR-041, FR-042): chi tiết một người và danh sách người cần đánh giá lại
 * cho Bác sĩ. Quyền xem đánh giá và mức chăm sóc; danh sách giới hạn theo phạm vi của người xem.
 */
@Service
public class ReassessmentQueryService {

    private static final PermissionKey VIEW_KEY = new PermissionKey(Function.ASSESSMENT_CARE_LEVEL, Operation.VIEW);

    /** Một người cần đánh giá lại kèm trạng thái và các căn cứ. */
    public record Entry(long residentId, Detail detail) {}

    private final ReassessmentNoticeService notices;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<ScopeApi> scope;

    ReassessmentQueryService(
            ReassessmentNoticeService notices,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions,
            ObjectProvider<ScopeApi> scope) {
        this.notices = notices;
        this.currentUser = currentUser;
        this.permissions = permissions;
        this.scope = scope;
    }

    @RequiresPermission(function = Function.ASSESSMENT_CARE_LEVEL, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Entry get(Target target) {
        return new Entry(target.residentId(), notices.detail(target.residentId()));
    }

    /**
     * Người đang cần hoặc quá hạn đánh giá lại trong phạm vi của người xem, sắp theo hạn (người chưa tính hạn, như đang
     * Điều trị tại bệnh viện, ở cuối). Danh sách đầy đủ vì số người cần đánh giá lại cùng lúc nhỏ (NFR-04).
     */
    @Transactional(readOnly = true)
    public List<Entry> due() {
        CurrentUser user = currentUser.getObject();
        if (!permissions.getObject().permissions(user).contains(VIEW_KEY)) {
            throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
        }
        ScopeFilter filter = scope.getObject().filter(user, VIEW_KEY);
        return notices.residentsWithUnmetNotices().stream()
                .filter(id -> filter.facilityWide() || filter.residentIds().contains(id))
                .map(id -> new Entry(id, notices.detail(id)))
                .filter(e -> e.detail().status().state() == ReassessmentState.DUE
                        || e.detail().status().state() == ReassessmentState.OVERDUE)
                .sorted(Comparator.comparing(
                                (Entry e) -> e.detail().status().dueAt().orElse(Instant.MAX))
                        .thenComparing(Entry::residentId))
                .toList();
    }
}
