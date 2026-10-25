package com.cambae.backend.m03.api.spi;

import com.cambae.backend.m03.api.dto.LocationRef;
import java.util.List;

/** Kiểm tham chiếu vị trí do 008 cài; module khác có thể cài thêm (FR-005). */
public interface LocationReferences {
    List<LocationReferenceView> currentReferences(LocationRef location);

    boolean everReferenced(LocationRef location);
}
