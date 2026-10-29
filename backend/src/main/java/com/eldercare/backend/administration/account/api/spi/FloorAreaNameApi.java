package com.eldercare.backend.administration.account.api.spi;

import java.util.Optional;

/**
 * SPI: tên tầng/khu vực để hiện cạnh mã tầng ở bản ghi chờ xem lại (FR-041c). Khai báo ở 002 để tránh vòng M15 ↔ M03;
 * M03 (003) cài (002 contracts/module-interfaces.md mục 2).
 */
public interface FloorAreaNameApi {

    /** Rỗng khi không có tầng/khu vực mang mã này. */
    Optional<String> name(long floorAreaId);
}
