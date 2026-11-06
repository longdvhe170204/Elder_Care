package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.dto.DayCareAbsenceNoticeView;
import com.eldercare.backend.admission.api.dto.DayCareCalendarView;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bản giả {@link DayCareCalendarApi} (004 cài) cho test của module khác (005): ngày chưa đặt thì báo lỗi, không đoán. Không
 * phải bean mặc định; test cần thì tự tạo hoặc đăng ký.
 */
public class FakeDayCareCalendarApi implements DayCareCalendarApi {

    private final Map<String, DayCareCalendarView> days = new HashMap<>();

    private static String key(long residentId, LocalDate date) {
        return residentId + ":" + date;
    }

    public FakeDayCareCalendarApi day(
            long residentId, LocalDate date, boolean closureDay, List<DayCareAbsenceNoticeView> notices) {
        days.put(key(residentId, date), new DayCareCalendarView(date, closureDay, notices));
        return this;
    }

    public FakeDayCareCalendarApi reset() {
        days.clear();
        return this;
    }

    @Override
    public DayCareCalendarView onDate(long residentId, LocalDate date) {
        DayCareCalendarView view = days.get(key(residentId, date));
        if (view == null) {
            throw new IllegalStateException(
                    "FakeDayCareCalendarApi chưa đặt ngày " + date + " của người " + residentId);
        }
        return view;
    }
}
