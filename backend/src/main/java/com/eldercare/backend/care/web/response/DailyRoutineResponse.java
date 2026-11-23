package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.dailyschedule.service.DailyRoutineService.Routine;
import java.time.LocalDate;

/** 005 FR-056: giá trị mới nhất của mốc và ngày bắt đầu áp dụng (thay đổi áp dụng từ ngày hôm sau). */
public record DailyRoutineResponse(
        long id, String name, String time, boolean active, LocalDate effectiveFrom, int version) {
    public static DailyRoutineResponse of(Routine r) {
        return new DailyRoutineResponse(
                r.id(), r.name(), r.time().toString(), r.active(), r.effectiveFrom(), r.version());
    }
}
