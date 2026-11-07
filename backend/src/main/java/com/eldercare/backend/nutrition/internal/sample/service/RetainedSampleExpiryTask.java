package com.eldercare.backend.nutrition.internal.sample.service;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.api.events.RetainedSampleNotice;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.RetainedSample;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobMarkers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08MinuteTask;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mẫu thức ăn quá lúc lưu + CFG-M08-06 mà chưa hủy thì nhắc bếp mức Nhẹ một lần mỗi mẫu (FR-062; dấu
 * {@code M08.JOB.RETAINED_SAMPLE_EXPIRY}). Xét mọi mẫu đã quá hạn nên lần chạy bù vẫn nhắc; hệ thống chỉ nhắc, không tự hủy.
 */
@Component
class RetainedSampleExpiryTask implements M08MinuteTask {

    private static final String KEY = "RETAINED_SAMPLE_EXPIRY";

    private final RetainedSampleService samples;
    private final M08JobMarkers markers;
    private final ApplicationEventPublisher events;

    RetainedSampleExpiryTask(RetainedSampleService samples, M08JobMarkers markers, ApplicationEventPublisher events) {
        this.samples = samples;
        this.markers = markers;
        this.events = events;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    @Transactional
    public void run(Instant now) {
        for (SimpleRecord<RetainedSample> sample : samples.overdue(now)) {
            if (markers.firstTime(KEY, RetainedSampleService.ENTITY, sample.id(), "due")) {
                events.publishEvent(new RetainedSampleNotice(
                        sample.id(), sample.content().dailyMealId(), RetainedSampleNotice.Kind.DISCARD_DUE, 0, now));
            }
        }
    }
}
