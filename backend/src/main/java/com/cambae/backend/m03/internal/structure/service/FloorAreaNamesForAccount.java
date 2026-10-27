package com.cambae.backend.m03.internal.structure.service;

import com.cambae.backend.m03.api.LocationStructureApi;
import com.cambae.backend.m03.api.dto.FloorView;
import com.cambae.backend.m15.account.api.spi.FloorAreaNameApi;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Bản cài SPI {@link FloorAreaNameApi} của 002 (tên tầng ở bản ghi chờ xem lại, FR-041c): tầng/khu vực của M15 là tầng
 * của M03. 002 khai báo SPI để tránh vòng M15 ↔ M03.
 */
@Service
class FloorAreaNamesForAccount implements FloorAreaNameApi {

    private final LocationStructureApi structure;

    FloorAreaNamesForAccount(LocationStructureApi structure) {
        this.structure = structure;
    }

    @Override
    public Optional<String> name(long floorAreaId) {
        return structure.floor(floorAreaId).map(FloorView::name);
    }
}
