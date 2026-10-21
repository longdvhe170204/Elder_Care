package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.spi.LocationReferences;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bản giả tham chiếu vị trí do 008 cài; bỏ tham chiếu hiện hành không xóa lịch sử (003 FR-005). */
public class FakeLocationReferences implements LocationReferences {

    private final Map<LocationRef, List<ReferenceView>> active = new HashMap<>();
    private final Set<LocationRef> historical = new HashSet<>();

    public FakeLocationReferences active(LocationRef location, List<ReferenceView> references) {
        active.put(location, List.copyOf(references));
        if (!references.isEmpty()) {
            historical.add(location);
        }
        return this;
    }

    /** Đặt tham chiếu đã kết thúc mà không cần tạo phân công giả của 008. */
    public FakeLocationReferences historical(LocationRef location) {
        historical.add(location);
        return this;
    }

    public void reset() {
        active.clear();
        historical.clear();
    }

    @Override
    public List<ReferenceView> activeReferences(LocationRef location) {
        return active.getOrDefault(location, List.of());
    }

    @Override
    public boolean everReferenced(LocationRef location) {
        return historical.contains(location);
    }
}
