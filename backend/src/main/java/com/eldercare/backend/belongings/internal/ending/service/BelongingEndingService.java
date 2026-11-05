package com.eldercare.backend.belongings.internal.ending.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.belongings.api.BelongingApi;
import com.eldercare.backend.belongings.api.dto.BelongingEndingConditionView;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.ending.domain.EndingConditionCalculator;
import com.eldercare.backend.belongings.internal.incident.service.BelongingIncidents;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc đồ gửi cho 004, 016 và màn kết thúc lưu trú ({@code DoGuiApi}; 013 T024, T026, FR-025, FR-026, BR-M12-03): điều
 * kiện (b) tính lúc hỏi từ trạng thái đồ nên luôn cập nhật sau lệnh làm đổi trạng thái (SC-006).
 */
@Service
public class BelongingEndingService implements BelongingApi {

    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final BelongingIncidents incidents;
    private final BelongingPostDeathItem postDeathItem;

    BelongingEndingService(
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            BelongingIncidents incidents,
            BelongingPostDeathItem postDeathItem) {
        this.belongings = belongings;
        this.handovers = handovers;
        this.incidents = incidents;
        this.postDeathItem = postDeathItem;
    }

    /** Điều kiện (b) và mục sau qua đời kèm căn cứ cho màn kết thúc lưu trú. */
    public record Ending(
            BelongingEndingConditionView condition,
            List<BelongingView> blocking,
            List<BelongingView> lostWithOpenIncident,
            PostDeathItemStatusView postDeathItem) {}

    @Override
    @Transactional(readOnly = true)
    public BelongingEndingConditionView endingCondition(long residentId) {
        return calculate(belongings.ofResident(residentId, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BelongingView> ofResident(long residentId) {
        return belongings.ofResident(residentId, null);
    }

    /** {@code GET /residents/{residentId}/belongings/ending-condition}: Quản lý viện, Trưởng tầng, Điều dưỡng, Hành chính xem. */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Ending ending(Target resident) {
        List<BelongingView> all = belongings.ofResident(resident.residentId(), null);
        BelongingEndingConditionView condition = calculate(all);
        List<BelongingView> blocking = all.stream()
                .filter(b -> condition.blockingBelongingIds().contains(b.belongingId()))
                .toList();
        List<BelongingView> lost = all.stream()
                .filter(b -> condition.lostWithOpenIncidentBelongingIds().contains(b.belongingId()))
                .toList();
        return new Ending(condition, blocking, lost, postDeathItem.status(resident.residentId()));
    }

    private BelongingEndingConditionView calculate(List<BelongingView> all) {
        return EndingConditionCalculator.evaluate(all.stream()
                .map(b -> new EndingConditionCalculator.Item(
                        b.belongingId(),
                        b.status(),
                        b.status() == BelongingStatus.LOST
                                && handovers
                                        .lossIncidentOf(b.belongingId())
                                        .map(incidents::isOpen)
                                        .orElse(false)))
                .toList());
    }
}
