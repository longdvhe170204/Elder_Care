package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreBreakdown;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Điểm ưu tiên (FR-009, BR-M02-10, research T-07): điểm mức chăm sóc + (số chu kỳ chờ trọn vẹn × điểm mỗi chu kỳ) + điểm các
 * tình huống đã xác nhận + tổng điều chỉnh đã duyệt. Số ngày chờ tính theo ngày lịch từ ngày đăng ký tới ngày tính; ví dụ
 * chờ 15 ngày với chu kỳ 7 ngày được 2 chu kỳ. Hàm thuần, không đọc cơ sở dữ liệu.
 */
public final class PriorityScoreCalculator {

    private PriorityScoreCalculator() {}

    public static ScoreBreakdown compute(
            PriorityScoreTable table,
            CareLevel careLevel,
            LocalDate applicationDate,
            LocalDate day,
            Set<String> confirmedCircumstances,
            int adjustment) {
        long waitedDays = Math.max(0, ChronoUnit.DAYS.between(applicationDate, day));
        long cycles = table.waitingPeriodDays() > 0 ? waitedDays / table.waitingPeriodDays() : 0;
        int circumstances =
                confirmedCircumstances.stream().mapToInt(table::pointsFor).sum();
        return new ScoreBreakdown(
                table.pointsFor(careLevel), (int) (cycles * table.pointsPerWaitingPeriod()), circumstances, adjustment);
    }
}
