package com.eldercare.backend.admission.api.spi;

import com.eldercare.backend.admission.api.dto.DepositStatusView;

/** SPI 017 cài (tên logic {@code TrangThaiDatCocPort}). Provisional: tên chờ E duyệt. */
public interface DepositStatusPort {

    /** FR-031: {@code NOT_REQUIRED}, {@code SATISFIED} hoặc {@code NOT_SATISFIED}, kèm căn cứ. */
    DepositStatusView status(long contractId);
}
