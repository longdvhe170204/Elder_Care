package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import com.eldercare.backend.care.internal.session.persistence.SessionTimetableStore;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/** 014 T006: ThoiKhoaBieuNguonPort (005 FR-048) cho buổi, chuyến đã đăng ký trong ngày; chỉ đọc. */
@Service
public class ActivityTimetableSource implements TimetableSourcePort {
    private final SessionTimetableStore store;

    public ActivityTimetableSource(SessionTimetableStore store) {
        this.store = store;
    }

    @Override
    public List<TimetableItemView> onDate(long residentId, LocalDate date) {
        return store.registered(residentId, date);
    }
}
