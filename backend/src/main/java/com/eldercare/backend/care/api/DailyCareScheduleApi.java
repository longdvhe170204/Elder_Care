package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.DailyScheduleEntry;
import java.time.LocalDate;
import java.util.List;

/**
 * 005 FR-055 (Q-440): lịch chăm sóc hằng ngày của một người cao tuổi, chỉ đọc. Không kiểm quyền: module gọi (012 cổng người
 * thân, kênh người cao tuổi) kiểm quan hệ và quyền xem sức khỏe rồi truyền {@code withMeasurements}.
 */
public interface DailyCareScheduleApi {
    List<DailyScheduleEntry> onDate(long residentId, LocalDate date, boolean withMeasurements);
}
