package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.staffing.api.spi.HeldBelongingView;
import com.eldercare.backend.staffing.api.spi.HeldBelongingsHandoverPort;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài {@code DoGuiBanGiaoPort} của 008 (013 T014; FR-014a, research K-11): đồ có giá trị (CFG-M12-01 lúc gọi) Đang
 * giữ do các nhân viên giữ, cho nguồn "đồ gửi đang giữ" của bàn giao ca (008 FR-039 (g), FR-040 (g)). Chỉ đọc; 013 không
 * ghi mục vào bàn giao. {@code @Fallback}: test của 008 tự khai bản giả {@code FakeHeldBelongings} (có nơi không
 * {@code @Primary}) thì bản giả được chọn; production chỉ có bean này (plan mục "Kế hoạch triển khai R-15" điểm 12).
 */
@Component
@Fallback
public class HeldBelongingsHandoverPortImpl implements HeldBelongingsHandoverPort {

    private final BelongingStore belongings;
    private final ValuableTypes valuableTypes;

    HeldBelongingsHandoverPortImpl(BelongingStore belongings, ValuableTypes valuableTypes) {
        this.belongings = belongings;
        this.valuableTypes = valuableTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeldBelongingView> heldBy(Collection<Long> staffIds) {
        return belongings.heldByStaff(valuableTypes.typeIds(), staffIds).stream()
                .map(b -> new HeldBelongingView(b.belongingId(), b.holderId(), b.residentId(), b.storageLocationId()))
                .toList();
    }

    /** Đồ còn Đang giữ bởi một trong các nhân viên; đồ đã chuyển, trả, thất lạc hoặc không có là {@code false}. */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> stillHeld(Collection<Long> belongingIds, Collection<Long> staffIds) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long id : belongingIds) {
            result.put(
                    id,
                    belongings
                            .find(id)
                            .filter(b -> b.status() == BelongingStatus.HELD
                                    && b.holderType() == HolderType.STAFF
                                    && staffIds.contains(b.holderId()))
                            .isPresent());
        }
        return Map.copyOf(result);
    }
}
