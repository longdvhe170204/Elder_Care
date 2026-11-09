package com.eldercare.backend.belongings.internal.ending.service;

import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverRow;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingRecalculation;
import com.eldercare.backend.belongings.internal.ending.domain.PostDeathBelongings;
import com.eldercare.backend.belongings.internal.ending.persistence.BelongingEndingStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài {@code MucViecSauQuaDoiPort} của 013 ({@code ma() = DO_GUI}; T024, FR-026, FR-026a, research K-08): mục "xử lý
 * đồ gửi" của danh sách việc sau qua đời, tính lúc hỏi từ bản ghi bàn giao hiện hành (sau đính chính, bỏ bản ghi Hủy
 * ghi nhận). Trạng thái tại lúc qua đời là trạng thái đích của bản ghi hiện hành mới nhất có thời điểm không muộn hơn
 * lúc qua đời.
 */
@Component
public class BelongingPostDeathItem implements PostDeathItemPort {

    private final ResidentApi residents;
    private final BelongingStore belongings;
    private final BelongingRecalculation recalculation;
    private final BelongingEndingStore endingStore;

    BelongingPostDeathItem(
            ResidentApi residents,
            BelongingStore belongings,
            BelongingRecalculation recalculation,
            BelongingEndingStore endingStore) {
        this.residents = residents;
        this.belongings = belongings;
        this.recalculation = recalculation;
        this.endingStore = endingStore;
    }

    @Override
    public PostDeathItemCode code() {
        return PostDeathItemCode.BELONGINGS;
    }

    @Override
    @Transactional(readOnly = true)
    public PostDeathItemStatusView status(long residentId) {
        Instant deathAt = residents.finalStatusAt(residentId).orElse(null);
        if (deathAt == null) {
            return new PostDeathItemStatusView(PostDeathItemStatus.NOT_COMPLETED, "Chưa có thời điểm qua đời (FR-026)");
        }
        List<BelongingView> all = belongings.ofResident(residentId, null);
        List<Long> ids = all.stream().map(BelongingView::belongingId).toList();
        Map<Long, BelongingStatus> atDeath = new HashMap<>();
        for (HandoverRow row : recalculation.effective(ids)) {
            if (!row.occurredAt().isAfter(deathAt)) {
                atDeath.put(row.belongingId(), row.targetStatus());
            }
        }
        Map<Long, Instant> tracked = endingStore.clockStarts(ids);
        return PostDeathBelongings.evaluate(all.stream()
                .map(b -> new PostDeathBelongings.Entry(
                        atDeath.get(b.belongingId()), b.status(), tracked.containsKey(b.belongingId())))
                .toList());
    }
}
