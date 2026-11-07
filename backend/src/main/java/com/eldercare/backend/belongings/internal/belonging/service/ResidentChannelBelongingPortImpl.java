package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.family.api.dto.ResidentChannelBelongingView;
import com.eldercare.backend.family.api.spi.ResidentChannelBelongingPort;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài {@code DoGuiKenhNctPort} của 013 (T033; FR-030a, Q-366, research K-16): đồ Đang giữ, Đang được sử dụng của
 * người cao tuổi cho kênh người cao tuổi của 012: tên, trạng thái, ảnh; không lịch sử bàn giao, không thao tác. Quyền
 * của 012 do bên gọi kiểm (tài khoản người cao tuổi xem chính mình).
 */
@Component
class ResidentChannelBelongingPortImpl implements ResidentChannelBelongingPort {

    private static final Set<BelongingStatus> VISIBLE = Set.of(BelongingStatus.HELD, BelongingStatus.IN_USE);

    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;

    ResidentChannelBelongingPortImpl(BelongingStore belongings, BelongingHandoverStore handovers) {
        this.belongings = belongings;
        this.handovers = handovers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentChannelBelongingView> held(long residentId) {
        return belongings.ofResident(residentId, null).stream()
                .filter(b -> VISIBLE.contains(b.status()))
                .map(this::view)
                .toList();
    }

    private ResidentChannelBelongingView view(BelongingView b) {
        Optional<Long> photo = belongings
                .intakeHandoverOf(b.belongingId())
                .map(handovers::ofIntake)
                .flatMap(history -> history.stream()
                        .filter(h -> h.belongingId() == b.belongingId()
                                && !h.photoFileIds().isEmpty())
                        .map(HandoverView::photoFileIds)
                        .map(List::getFirst)
                        .findFirst());
        return new ResidentChannelBelongingView(
                b.belongingId(), b.description(), b.status().name(), photo);
    }
}
