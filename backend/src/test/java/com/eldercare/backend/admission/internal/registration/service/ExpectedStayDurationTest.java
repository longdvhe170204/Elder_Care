package com.eldercare.backend.admission.internal.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.registration.service.ExpectedStayDuration.StayPeriod;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 018 T003 (FR-001, FR-001a, FR-007; BR-M02-11, Q-215, Q-241, Q-336; research Z-03): tổng thời gian lưu trú dự kiến tính
 * từ ngày bắt đầu hợp đồng đầu của chuỗi nối tiếp không gián đoạn (hoặc ngày hiệu lực phần nội trú) tới ngày kết thúc dự
 * kiến mới nhất, gồm phụ lục gia hạn; đếm cả ngày đầu và ngày cuối (chủ dự án chốt 2026-10-27).
 */
class ExpectedStayDurationTest {

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static StayPeriod period(String start, String end) {
        return new StayPeriod(d(start), end == null ? null : d(end));
    }

    @Test
    void shortStayOf14Days_isFourteenDays() {
        assertThat(ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-14")), null)
                        .days())
                .hasValue(14);
    }

    @Test
    void oneDayContract_isOneDay() {
        assertThat(ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-01")), null)
                        .days())
                .hasValue(1);
    }

    /** Phụ lục gia hạn đổi ngày kết thúc dự kiến của hợp đồng; bên gọi truyền ngày đã gồm gia hạn (US3 #6: 14 + 7). */
    @Test
    void contractExtendedByAddendum_usesTheExtendedEnd() {
        assertThat(ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-21")), null)
                        .days())
                .hasValue(21);
    }
}
