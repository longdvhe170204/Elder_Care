package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.DoseDisplayView;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * API đọc lịch thuốc tính từ đơn (LichThuocApi).
 * Dùng bởi Module M04 (005 qua SPI checklist), M09 (008), M14 (016).
 */
public interface MedicationScheduleApi {
    /** Lịch dùng thuốc của một người cao tuổi trong khoảng thời gian. */
    List<DoseDisplayView> byResident(long residentId, Instant from, Instant to);

    /** Danh sách id nhân viên đã ghi nhận liều hoặc lần dùng PRN trong khoảng (007 FR-060c). */
    Set<Long> staffRecordedDose(long residentId, Instant from, Instant to);
}
