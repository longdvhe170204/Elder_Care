package com.eldercare.backend.admission.api.spi;

import java.time.LocalDate;
import java.util.Optional;

/** SPI 012 cài (tên logic {@code NguoiDaiDienPort}). Provisional: tên chờ E duyệt. */
public interface RepresentativePort {

    /** DBR-02, FR-021, FR-039: người thân là người đại diện hiệu lực của người cao tuổi tại ngày đó. */
    boolean isActiveRepresentative(long familyMemberId, long residentId, LocalDate date);

    /** Lập hợp đồng cần người cao tuổi đã có người đại diện. */
    boolean hasRepresentative(long residentId);

    /**
     * FR-034, BR-M11-22 của 017 (chủ dự án duyệt 2026-11-10): mã người thân là người đại diện hiệu lực hiện tại của người
     * cao tuổi; họ tên đọc qua {@code FamilyRelationshipApi.familyMemberName}.
     */
    Optional<Long> activeRepresentative(long residentId);
}
