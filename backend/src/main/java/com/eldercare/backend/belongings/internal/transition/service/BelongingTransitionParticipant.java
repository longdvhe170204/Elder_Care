package com.eldercare.backend.belongings.internal.transition.service;

import com.eldercare.backend.belongings.api.dto.BelongingEndingConditionView;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.ending.service.BelongingEndingService;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Bean {@code ThamGiaChuyenTrangThai} của 013 (T024; contracts mục 1.1, FR-025, BR-M12-03): điều kiện {@code
 * END_B_BELONGINGS} của lệnh Kết thúc lưu trú, miễn được qua ngoại lệ của 004 (FR-064). Thứ tự sau 012. Không có tác
 * động: chặn Tiếp nhận, Giao sử dụng sau trạng thái cuối (FR-027) đã nằm ở kiểm lệnh bàn giao. Đồ Thất lạc không làm
 * Chưa đạt (Q-148).
 */
@Component
@Order(80)
class BelongingTransitionParticipant implements StatusTransitionParticipant {

    static final String CODE = "END_B_BELONGINGS";

    private final BelongingEndingService ending;
    private final BelongingStore belongings;
    private final BelongingResidentLock residentLock;

    BelongingTransitionParticipant(
            BelongingEndingService ending, BelongingStore belongings, BelongingResidentLock residentLock) {
        this.ending = ending;
        this.belongings = belongings;
        this.residentLock = residentLock;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.END_RESIDENCY);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.END_RESIDENCY) {
            return List.of();
        }
        if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            // K-03: lệnh Kết thúc lưu trú đọc dưới khóa do_gui_nct (sau khóa hồ sơ của 001) để không lệch với lệnh đang
            // ghi trên đồ; bản xem trước chạy transaction chỉ đọc nên không khóa
            residentLock.lock(context.residentId());
        }
        BelongingEndingConditionView view = ending.endingCondition(context.residentId());
        if (view.met()) {
            return List.of();
        }
        String items = view.blockingBelongingIds().stream()
                .map(id -> belongings.find(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(BelongingTransitionParticipant::describe)
                .collect(Collectors.joining("; "));
        return List.of(new Condition(
                CODE, "Còn " + view.blockingBelongingIds().size() + " đồ gửi chưa trả: " + items, "BR-M12-03", true));
    }

    @Override
    public void applyEffects(TransitionResult result) {}

    private static String describe(BelongingView b) {
        return b.description() + " (" + statusName(b.status()) + ")";
    }

    private static String statusName(BelongingStatus status) {
        return switch (status) {
            case HELD -> "Đang giữ";
            case IN_USE -> "Đang được sử dụng";
            case DAMAGED -> "Hư hỏng";
            default -> status.name();
        };
    }
}
