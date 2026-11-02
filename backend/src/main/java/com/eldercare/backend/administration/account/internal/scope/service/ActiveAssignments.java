package com.eldercare.backend.administration.account.internal.scope.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.AssignmentApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Phân công góp vào phạm vi tại một thời điểm (002 T052; FR-032, research A-07), dùng chung cho phạm vi và quyền theo
 * nhiệm vụ (T053): phân công gắn ca góp trong {@code [bắt đầu ca − CFG-M15-07, kết thúc ca + CFG-M15-07)}; phân công
 * không gắn ca góp trong thời gian hiệu lực. Không đệm giữa các lần gọi (FR-033).
 */
@Component
public class ActiveAssignments {

    // ObjectProvider: 008, 003 đã cài; context không có bean (test không nạp module đó) thì dùng mặc định an toàn
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<BedAllocationApi> bedAllocations;
    private final ParameterApi parameters;

    ActiveAssignments(
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<BedAllocationApi> bedAllocations,
            ParameterApi parameters) {
        this.assignments = assignments;
        this.bedAllocations = bedAllocations;
        this.parameters = parameters;
    }

    /** Khoảng CFG-M15-07 trước và sau giờ ca. */
    public Duration margin() {
        return parameters.read(ParameterCode.CFG_M15_07).asDuration();
    }

    /** Phân công của nhân viên góp vào phạm vi tại {@code at}. */
    public List<AssignmentView> at(long staffId, Instant at) {
        Duration margin = margin();
        // DATETIME(6): lấy dư một microgiây để không mất đầu biên; covers vẫn kiểm đúng thời điểm.
        return history(staffId, at.minus(margin), at.plus(margin).plusNanos(1000)).stream()
                .filter(a -> covers(a, at, margin))
                .toList();
    }

    /** Phân công có khoảng góp giao với {@code [from, to)} (FR-035a). */
    public List<AssignmentView> during(long staffId, Instant from, Instant to) {
        Duration margin = margin();
        return history(staffId, from.minus(margin), to.plus(margin)).stream()
                .filter(a -> {
                    Duration m = a.shiftId() != null ? margin : Duration.ZERO;
                    return a.from().minus(m).isBefore(to)
                            && (a.to() == null || a.to().plus(m).isAfter(from))
                            && (a.scopeEndsAt() == null || a.scopeEndsAt().isAfter(from));
                })
                .toList();
    }

    // Không có bean AssignmentApi thì không có phân công nào góp vào phạm vi (mặc định an toàn, như bedOf), không ném
    // lỗi
    private List<AssignmentView> history(long staffId, Instant from, Instant to) {
        AssignmentApi api = assignments.getIfAvailable();
        return api == null ? List.of() : api.history(staffId, from, to);
    }

    /** Phân bổ giường đang mở của người cao tuổi tại {@code at}. */
    public Optional<BedAllocationView> bedOf(long residentId, Instant at) {
        BedAllocationApi api = bedAllocations.getIfAvailable();
        return api == null ? Optional.empty() : api.openAt(residentId, at);
    }

    /**
     * Tầng/khu vực của đối tượng: tầng ghi trên đối tượng, không có thì tầng của phân bổ giường đang mở của người cao tuổi
     * tại {@code at}; rỗng khi không xác định được (bản ghi không gắn tầng).
     */
    public Optional<Long> floorOf(Target target, Instant at) {
        if (target.floorAreaId() != null) {
            return Optional.of(target.floorAreaId());
        }
        return target.residentId() == null
                ? Optional.empty()
                : bedOf(target.residentId(), at).map(BedAllocationView::floorAreaId);
    }

    /** Như {@link #floorOf} nhưng dữ liệu nguồn lỗi thì coi như không gắn tầng (chỉ để chọn người nhận báo). */
    public Optional<Long> floorOrEmpty(Target target, Instant at) {
        try {
            return floorOf(target, at);
        } catch (InvalidPermissionDataException e) {
            return Optional.empty();
        }
    }

    static boolean covers(AssignmentView a, Instant at, Duration margin) {
        Duration m = a.shiftId() != null ? margin : Duration.ZERO;
        return !at.isBefore(a.from().minus(m))
                && (a.to() == null || at.isBefore(a.to().plus(m)))
                && (a.scopeEndsAt() == null || at.isBefore(a.scopeEndsAt()));
    }
}
