package com.cambae.backend.m15.account.scope.internal;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AssignmentApi;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.BedAllocationApi;
import com.cambae.backend.m15.account.api.BedAllocationView;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.ReviewRecipient;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.ScopeFilter;
import com.cambae.backend.m15.account.api.Target;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;

/**
 * Phạm vi dữ liệu (002 research A-07). Phần đã cài (kéo lên US1 cho T018, chủ dự án đồng ý 2026-10-20):
 * {@link #inScope} của nhân viên, tính trong từng lần gọi từ phân công đang hiệu lực (008) và phân bổ giường đang mở
 * (003): đối tượng trong phạm vi khi một phân công trùng người cao tuổi, phòng hoặc tầng/khu vực của đối tượng. Còn lại
 * ở T052 (US4): khoảng CFG-M15-07 quanh ca, {@code filter}, {@code historyFilter}, người phụ trách ca, trưởng tầng được
 * giao, người quyết định xem lại; phạm vi theo quan hệ của người thân ở T071 (US6), của người cao tuổi ở T091 (US8).
 */
@Service
@Fallback
class ScopeService implements ScopeApi {

    private static final String LATER = "Cài ở 002 T052 (US4)";

    private final BusinessTime time;
    // ObjectProvider: 008, 003 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<BedAllocationApi> bedAllocations;

    ScopeService(
            BusinessTime time,
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<BedAllocationApi> bedAllocations) {
        this.time = time;
        this.assignments = assignments;
        this.bedAllocations = bedAllocations;
    }

    @Override
    public boolean inScope(CurrentUser user, Target target) {
        if (user.isScheduler()) {
            return true;
        }
        if (user.staffId().isEmpty()) {
            return false;
        }
        Instant now = time.now();
        Long floorAreaId = target.floorAreaId();
        Long roomId = null;
        if (target.residentId() != null) {
            Optional<BedAllocationView> bed = bedAllocations.getObject().openAt(target.residentId(), now);
            if (bed.isPresent()) {
                floorAreaId = floorAreaId != null ? floorAreaId : bed.get().floorAreaId();
                roomId = bed.get().roomId();
            }
        }
        for (AssignmentView a : assignments.getObject().activeAt(user.staffId().get(), now)) {
            if (matches(a.residentId(), target.residentId())
                    || matches(a.roomId(), roomId)
                    || matches(a.floorAreaId(), floorAreaId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Long assigned, Long ofTarget) {
        return assigned != null && Objects.equals(assigned, ofTarget);
    }

    @Override
    public ScopeFilter filter(CurrentUser user, PermissionKey key) {
        throw new UnsupportedOperationException(LATER);
    }

    @Override
    public ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to) {
        throw new UnsupportedOperationException(LATER);
    }

    @Override
    public Optional<Long> shiftLead(long floorAreaId, Instant at) {
        throw new UnsupportedOperationException(LATER);
    }

    @Override
    public Optional<Long> assignedFloorManager(long floorAreaId) {
        throw new UnsupportedOperationException(LATER);
    }

    @Override
    public ReviewRecipient reviewDecider(Optional<Long> floorAreaId) {
        throw new UnsupportedOperationException(LATER);
    }
}
