package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions.Transition;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.swap.domain.SwapStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu {@code M09.SHIFT_SWAP} (đổi ca, UC-54) của 000 với vòng đời mở rộng (research X-08; contracts
 * module-interfaces mục 2.2): các trạng thái "Chờ người nhận đồng ý", "Người nhận từ chối", "Hết hiệu lực" là chuyển mở rộng
 * do lệnh của 015 kích hoạt. Duyệt, từ chối duyệt, áp dụng đi qua {@link SwapService}, nên lệnh duyệt chung của 000 bị
 * chặn ở {@link #checkOnApprove} (D tự chọn 2026-11-09: một đường duy nhất để người duyệt chọn người phụ trách và xác nhận
 * cảnh báo).
 */
@Component
class SwapHandler implements RequestTypeHandler {

    static final String TYPE = "M09.SHIFT_SWAP";
    static final String WAIT_RECIPIENT = "WAIT_RECIPIENT";
    static final String RECIPIENT_AGREED = "RECIPIENT_AGREED";
    static final String RECIPIENT_DECLINED = "RECIPIENT_DECLINED";
    static final String CANCEL = "CANCEL";
    static final String EXPIRE = "EXPIRE";
    static final String APPLY = "APPLY";
    static final String REJECT = "REJECT";

    private final ShiftStore shifts;

    SwapHandler(ShiftStore shifts) {
        this.shifts = shifts;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        for (String field : List.of("kind", "shiftXId", "recipientId", "giverStaffId")) {
            if (!content.hasNonNull(field)) {
                throw new InvalidValueException("Thiếu trường " + field);
            }
        }
        if (!Set.of("MUTUAL", "TAKE_OVER").contains(content.get("kind").asString(""))) {
            throw new InvalidValueException("Dạng đổi ca không hợp lệ");
        }
    }

    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.CREATE);
    }

    /**
     * FR-019 (015 T033): người lập là người nhường và có tên trong ca X; không cần phân công hiện tại (ca X chưa bắt
     * đầu).
     */
    @Override
    public Optional<Boolean> creatorInOwnScope(CurrentUser user, JsonNode content) {
        long giver = content.path("giverStaffId").asLong();
        long shiftX = content.path("shiftXId").asLong();
        return Optional.of(user.staffId().filter(id -> id == giver).isPresent()
                && shifts.members(shiftX).stream().anyMatch(m -> m.staffId() == giver));
    }

    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        long shiftId = content.get("shiftXId").asLong();
        // Tầng/khu vực của ca X lấy ở SwapService.scopeOf trước khi lập; ở đây chỉ giữ mã ca.
        return new Target(
                "M09.SHIFT",
                Long.toString(shiftId),
                null,
                content.hasNonNull("floorAreaId") ? content.get("floorAreaId").asLong() : null);
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        return List.of();
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        return new ResultRef(TYPE, Long.toString(request.id()));
    }

    @Override
    public List<UnmetCondition> checkOnApprove(ApprovalRequestView request) {
        return List.of(new UnmetCondition(
                "USE_SHIFT_SWAP_ENDPOINT", "Duyệt đổi ca qua POST /shift-swaps/{id}/approve của lịch ca"));
    }

    @Override
    public ExtendedTransitions extendedTransitions() {
        Set<String> open = Set.of(SwapStatus.AWAITING_RECIPIENT, SwapStatus.PENDING_APPROVAL);
        return new ExtendedTransitions(Map.of(
                WAIT_RECIPIENT, new Transition(Set.of(SwapStatus.PENDING_APPROVAL), SwapStatus.AWAITING_RECIPIENT),
                RECIPIENT_AGREED, new Transition(Set.of(SwapStatus.AWAITING_RECIPIENT), SwapStatus.PENDING_APPROVAL),
                RECIPIENT_DECLINED, new Transition(open, SwapStatus.RECIPIENT_DECLINED),
                CANCEL, new Transition(open, SwapStatus.CANCELLED),
                EXPIRE, new Transition(open, SwapStatus.EXPIRED),
                APPLY, new Transition(Set.of(SwapStatus.PENDING_APPROVAL), SwapStatus.APPLIED),
                REJECT, new Transition(Set.of(SwapStatus.PENDING_APPROVAL), SwapStatus.REJECTED)));
    }
}
