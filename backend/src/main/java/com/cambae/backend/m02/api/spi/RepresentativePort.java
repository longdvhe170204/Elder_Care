package com.cambae.backend.m02.api.spi;

import java.time.LocalDate;

/** SPI 012 cài (tên logic {@code NguoiDaiDienPort}). Provisional: tên chờ E duyệt. */
public interface RepresentativePort {

    /** DBR-02, FR-021, FR-039: người thân là người đại diện hiệu lực của người cao tuổi tại ngày đó. */
    boolean isActiveRepresentative(long familyMemberId, long residentId, LocalDate date);

    /** Lập hợp đồng cần người cao tuổi đã có người đại diện. */
    boolean hasRepresentative(long residentId);
}
