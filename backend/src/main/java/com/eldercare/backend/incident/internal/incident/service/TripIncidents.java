package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.care.api.spi.TripIncidentPort;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link TripIncidentPort} (014 FR-046 → 048, BR-M04-17; 007 T063, 2026-11-11, A thay D): sự cố "Đi lạc hoặc không
 * trở về", Khẩn cấp, nguồn hoạt động ngoài viện, ghi bằng người đang ra lệnh Báo thiếu người qua cùng luồng ghi nhận
 * REST (quyền, phạm vi, chặn sự cố Khẩn cấp đang mở). Tìm thấy, đã trở về ghi diễn biến Đã tìm thấy do Hệ thống (tác
 * động kèm lệnh điểm danh về), không tự đóng. Lỗi thì lệnh chuyến rollback (MANDATORY).
 */
@Component
class TripIncidents implements TripIncidentPort {
    static final String TYPE = "MISSING_OR_NOT_RETURNED";
    static final String CONTEXT = "OFFSITE_ACTIVITY";
    static final String FOUND = "FOUND";

    private final IncidentRecordService records;
    private final IncidentProgressionService progressions;
    private final IncidentCatalogService catalog;
    private final CurrentUser user;
    private final BusinessTime time;

    TripIncidents(
            IncidentRecordService records,
            IncidentProgressionService progressions,
            IncidentCatalogService catalog,
            CurrentUser user,
            BusinessTime time) {
        this.records = records;
        this.progressions = progressions;
        this.catalog = catalog;
        this.user = user;
        this.time = time;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long recordMissing(
            long residentId, long tripId, Long leaderId, String lastSeenPlace, Instant lastSeenAt, String description) {
        long type = catalog.incidentTypes().stream()
                .filter(t -> TYPE.equals(t.code()))
                .map(IncidentTypeView::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu loại sự cố " + TYPE));
        return records.record(
                        new RecordIncidentCommand(
                                residentId,
                                type,
                                CONTEXT,
                                lastSeenAt,
                                time.now(),
                                null,
                                lastSeenPlace,
                                "Chuyến đi " + tripId,
                                description,
                                "CRITICAL",
                                null,
                                null,
                                false),
                        user)
                .incidentId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordFoundReturned(long incidentId, long residentId, long tripId, Instant at) {
        progressions.addBySystem(incidentId, FOUND, "Đã tìm thấy, đã trở về (chuyến đi " + tripId + ")", at);
    }
}
