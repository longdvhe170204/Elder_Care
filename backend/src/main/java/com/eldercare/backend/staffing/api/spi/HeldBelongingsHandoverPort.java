package com.eldercare.backend.staffing.api.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 013 cài; chỉ đọc đồ có giá trị Đang giữ cho nguồn bàn giao FR-039 (g), FR-040 (g). */
public interface HeldBelongingsHandoverPort {

    List<HeldBelongingView> heldBy(Collection<Long> staffIds);

    Map<Long, Boolean> stillHeld(Collection<Long> belongingIds, Collection<Long> staffIds);
}
