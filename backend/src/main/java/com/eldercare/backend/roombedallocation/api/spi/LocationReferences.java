package com.eldercare.backend.roombedallocation.api.spi;

import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import java.util.List;

/** Kiểm tham chiếu vị trí do 008 cài; module khác có thể cài thêm (FR-005). */
public interface LocationReferences {
    List<LocationReferenceView> currentReferences(LocationRef location);

    boolean everReferenced(LocationRef location);
}
