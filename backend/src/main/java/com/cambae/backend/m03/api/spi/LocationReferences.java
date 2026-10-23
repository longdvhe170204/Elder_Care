package com.cambae.backend.m03.api.spi;

import com.cambae.backend.m03.api.dto.*;
import java.util.List;

/** ThamChieuViTri; 008 cài, chủ 003 review theo duyệt của người dùng 2026-10-24. */
public interface LocationReferences {
    List<LocationReferenceView> activeReferences(LocationRef location);

    boolean everReferenced(LocationRef location);
}
