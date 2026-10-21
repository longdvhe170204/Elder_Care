package com.cambae.backend.m03.api.spi;

import com.cambae.backend.m03.api.dto.LocationRef;
import java.util.List;

/** 008 cài, module khác có thể cài; kiểm tham chiếu khi xóa/ngừng hiệu lực cơ cấu (003 FR-005 c). */
public interface LocationReferences {
    List<ReferenceView> activeReferences(LocationRef location);

    boolean everReferenced(LocationRef location);

    /** Đủ để chỉ ra đối tượng gây chặn, không chứa bản ghi nội bộ của module nguồn. */
    record ReferenceView(String type, long id, String label) {}
}
