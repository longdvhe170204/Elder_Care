package com.cambae.backend.support.fake.m09;

import com.cambae.backend.m09.api.spi.HeldBelongingView;
import com.cambae.backend.m09.api.spi.HeldBelongingsHandoverPort;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nguồn đồ gửi giả; test đặt đồ đang giữ hoặc bỏ đồ, không kiểm luật hay ghi dữ liệu của 013. */
public class FakeHeldBelongings implements HeldBelongingsHandoverPort {

    private final Map<Long, HeldBelongingView> belongings = new LinkedHashMap<>();
    private boolean available = true;

    public FakeHeldBelongings hold(HeldBelongingView belonging) {
        belongings.put(belonging.belongingId(), belonging);
        return this;
    }

    public FakeHeldBelongings release(long belongingId) {
        belongings.remove(belongingId);
        return this;
    }

    public FakeHeldBelongings available(boolean value) {
        available = value;
        return this;
    }

    @Override
    public List<HeldBelongingView> heldBy(Collection<Long> staffIds) {
        requireAvailable();
        return belongings.values().stream()
                .filter(belonging -> staffIds.contains(belonging.holderStaffId()))
                .toList();
    }

    @Override
    public Map<Long, Boolean> stillHeld(Collection<Long> belongingIds, Collection<Long> staffIds) {
        requireAvailable();
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long id : belongingIds) {
            HeldBelongingView belonging = belongings.get(id);
            result.put(id, belonging != null && staffIds.contains(belonging.holderStaffId()));
        }
        return Map.copyOf(result);
    }

    private void requireAvailable() {
        if (!available) {
            throw new IllegalStateException("Nguồn đồ gửi giả chưa có dữ liệu");
        }
    }
}
