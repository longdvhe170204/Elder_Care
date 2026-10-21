package com.cambae.backend.m03.api.spi;

import com.cambae.backend.m03.api.dto.CareLevel;
import com.cambae.backend.m03.api.dto.ResidencyType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/** 004 cài; nguồn dữ liệu lưu trú cho phân bổ giường (003 contracts mục 2.1). */
public interface AllocationResidencySource {

    /** Hợp đồng Hiệu lực → hợp đồng đang lập → lượt đăng ký (CHK007). */
    Optional<ResidencyType> residencyType(long residentId);

    /** Mức của hợp đồng Nháp, Chờ ký hoặc Hiệu lực (Q-301). */
    Optional<CareLevel> contractCareLevel(long residentId);

    Optional<CareLevel> registrationCareLevel(long residentId);

    Optional<Instant> expectedEnd(long residentId);

    /** Đọc trong lệnh vắng, sau phần xử lý của 004. */
    BedRetention bedRetentionWhenAbsent(long residentId);

    boolean differentUnitPrice(long residentId, long oldRoomTypeId, long newRoomTypeId, LocalDate date);

    /** Bản cài phải chạy trong transaction hiện có (MANDATORY; 003 FR-015). */
    void waitingFileHoldExpired(long waitingFileId);

    /** Bản cài phải chạy trong transaction hiện có (MANDATORY; 004 FR-016). */
    void allocated(long residentId, long allocationId);

    /** Giữ giường có thể không có hạn; không giữ thì retained = false. */
    record BedRetention(boolean retained, Optional<Instant> until) {}
}
