package com.cambae.backend.m02.api.spi;

import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatusView;

/**
 * SPI 010, 013, 006, 007, 017 cài, mỗi bên một bean (tên logic {@code MucViecSauQuaDoiPort}). Provisional: tên chờ A,
 * D, E duyệt.
 */
public interface PostDeathItemPort {

    PostDeathItemCode code();

    /** FR-071: tính tại lúc xem. */
    PostDeathItemStatusView status(long residentId);
}
