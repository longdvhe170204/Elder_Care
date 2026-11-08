package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import java.time.LocalDate;

/** T051/FR-037: 007 bỏ kết quả ngủ đã Hủy ghi nhận khi tính lại; không sửa bản gốc. */
public record SleepResultVoided(long resultId, long residentId, LocalDate date, CareEventContext context)
        implements CareEvent {}
