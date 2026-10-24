package com.cambae.backend.m01.internal.health.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.events.AllergyRecorded;
import com.cambae.backend.m01.internal.health.domain.HealthItem;
import com.cambae.backend.m01.internal.health.persistence.HealthItemRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phát lại {@link AllergyRecorded} cho dị ứng Hiệu lực còn cờ chờ kiểm tra quá 15 phút (001 T031; BR-M01-12; research
 * H-07, H-12), tới khi 006 và 011 gỡ cả hai cờ. Listener của hai module idempotent theo {@code itemId}. Chỉ phát sự
 * kiện, không tạo bản ghi nghiệp vụ.
 */
@Service
public class AllergyCheckRetry {

    public static final String JOB = "RETRY_ALLERGY_CHECKS";

    private final HealthItemRepository items;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    // Chu kỳ kỹ thuật của job (research H-12), không phải mốc nghiệp vụ nên đặt ở cấu hình, không ở ParameterApi
    private final Duration overdueAfter;

    AllergyCheckRetry(
            HealthItemRepository items,
            ApplicationEventPublisher events,
            BusinessTime time,
            @Value("${eldercare.jobs.retry-allergy-checks.overdue-after:PT15M}") Duration overdueAfter) {
        this.items = items;
        this.events = events;
        this.time = time;
        this.overdueAfter = overdueAfter;
    }

    /** Trả số mục đã phát lại. */
    @Transactional
    public int run() {
        List<HealthItem> pending = items.findStillPending(time.now().minus(overdueAfter));
        pending.forEach(i -> events.publishEvent(new AllergyRecorded(i.getId(), i.getResidentId(), i.getAllergenId())));
        return pending.size();
    }
}
