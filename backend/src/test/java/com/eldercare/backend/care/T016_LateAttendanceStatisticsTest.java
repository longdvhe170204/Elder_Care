package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.care.api.dto.SessionStatisticsView;
import com.eldercare.backend.care.internal.activity.persistence.ActivityStatisticsStore;
import com.eldercare.backend.care.internal.activity.service.ActivityStatistics;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 016 T016 (016 FR-032; 014 bảng giao tiếp "Gửi Module 14": nhãn "ghi nhận muộn" của điểm danh, 014 FR-030): mỗi buổi trả
 * số điểm danh mang nhãn ghi nhận muộn, không tính bản ghi đã Hủy ghi nhận.
 */
class T016_LateAttendanceStatisticsTest {

    private static Map<String, Object> attendance(long id, String result, String late) {
        var row = new HashMap<String, Object>();
        row.put("id", id);
        row.put("session_id", 1L);
        row.put("result", result);
        row.put("absence_reason", null);
        row.put("late_recorded", late);
        return row;
    }

    @Test
    void FR_032_sessionCountsLateRecordedAttendance_exceptVoided() {
        ActivityStatisticsStore store = mock(ActivityStatisticsStore.class);
        CorrectionApi corrections = mock(CorrectionApi.class);
        var session = new HashMap<String, Object>();
        session.put("id", 1L);
        session.put("activity_id", 5L);
        session.put("floor_area_id", 101L);
        session.put("starts_at", Timestamp.from(Instant.parse("2026-11-01T02:00:00Z")));
        session.put("status", "ATTENDANCE_TAKEN");
        session.put("trip_status", null);
        session.put("registered", 4);
        when(store.sessions(any(), any(), any())).thenReturn(List.of(session));
        when(store.attendance(anyCollection()))
                .thenReturn(List.of(
                        attendance(11, "PRESENT", "true"),
                        attendance(12, "PRESENT", "false"),
                        attendance(13, "ABSENT", "true"),
                        attendance(14, "PRESENT", "true")));
        CurrentValue voided = new CurrentValue(null, false, true);
        when(corrections.current(eq("M04.SESSION_ATTENDANCE"), anyCollection())).thenReturn(Map.of(14L, voided));

        var statistics = new ActivityStatistics(store, corrections, mock(RecordEventApi.class));
        List<SessionStatisticsView> views = statistics
                .sessions(ScopeFilter.FACILITY, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-01"))
                .toList();

        assertThat(views).singleElement().satisfies(v -> {
            assertThat(v.present()).isEqualTo(2);
            assertThat(v.lateRecorded()).isEqualTo(2);
        });
    }
}
