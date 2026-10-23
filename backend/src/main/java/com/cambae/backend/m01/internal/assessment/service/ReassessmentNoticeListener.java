package com.cambae.backend.m01.internal.assessment.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Nghe sự kiện nội bộ của M01 để thêm lần báo đánh giá lại (001 T063). Chạy đồng bộ trong transaction của lệnh nguồn nên
 * lần báo và đính chính cùng thành công hoặc cùng hoàn tác. Sự kiện {@code IncidentRecorded} của 007 (căn cứ sau sự cố,
 * khi {@code canDanhGiaLai}) chưa có để nghe: D chưa khai báo; xem {@link ReassessmentNoticeService#notifyAfterIncident}.
 */
@Component
class ReassessmentNoticeListener {

    private final ReassessmentNoticeService notices;

    ReassessmentNoticeListener(ReassessmentNoticeService notices) {
        this.notices = notices;
    }

    @EventListener
    void on(AssessmentCorrectedLevel event) {
        notices.notifyAfterCorrection(event.residentId(), event.correctionId());
    }
}
