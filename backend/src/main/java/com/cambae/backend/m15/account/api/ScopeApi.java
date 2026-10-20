package com.cambae.backend.m15.account.api;

import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.ReviewRecipient;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.dto.Target;
import java.time.Instant;
import java.util.Optional;

/**
 * Phạm vi dữ liệu, tính trong từng yêu cầu (FR-028 → FR-035a; 002 research A-07). Người phụ trách ca, trưởng tầng được
 * giao lấy từ 008. 000 khai báo trước (000 T025); 002 thêm các hàm còn lại của contracts/module-interfaces.md mục 1.3
 * (T008).
 */
public interface ScopeApi {

    boolean inScope(CurrentUser user, Target target);

    /**
     * Như {@link #inScope(CurrentUser, Target)} theo phân công và phân bổ giường tại thời điểm đã cho (research A-07
     * {@code trongPhamVi(nd, doiTuong, thoiDiem)}), cho kiểm tại thời điểm thiết bị (FR-041a). Mặc định là phạm vi hiện
     * hành, để bản giả của test cũ không phải cài.
     */
    default boolean inScope(CurrentUser user, Target target, Instant at) {
        return inScope(user, target);
    }

    /** Bộ lọc phạm vi hiện hành của khóa quyền (FR-035). */
    ScopeFilter filter(CurrentUser user, PermissionKey key);

    /** Bộ lọc dựng từ lịch sử phân công trong khoảng đã qua, chỉ để đọc (FR-035a); không gọi từ lệnh nghiệp vụ. */
    ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to);

    /** Tài khoản người phụ trách ca của tầng/khu vực tại thời điểm đã cho. */
    Optional<Long> shiftLead(long floorAreaId, Instant at);

    /** Tài khoản trưởng tầng được giao của tầng/khu vực. */
    Optional<Long> assignedFloorManager(long floorAreaId);

    /** Người quyết định xem lại; rỗng tầng/khu vực là bản ghi không gắn tầng (FR-041c, CHK056). */
    ReviewRecipient reviewDecider(Optional<Long> floorAreaId);
}
