package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.m02.api.events.ResidencyDaysChanged;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m02.internal.transition.service.M02DailyTask;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code ENDINGS} của job hằng ngày (FR-066a, FR-077, BR-M02-19; research T-11): hồ sơ kết thúc còn Đang chuẩn bị mà ngày
 * kết thúc dự kiến đã qua thì gắn dấu "quá ngày dự kiến" từ ngày liền sau (đúng ngày lẽ ra phải gắn, kể cả khi job chạy bù) và
 * báo 010 các ngày từ đó lại là sự kiện tính phí; mỗi ngày còn quá thì báo ngày đó. Nhắc Hành chính ở {@link EndingReminders}.
 * Không tự đổi ngày, không tự Kết thúc lưu trú (Q-306, Q-307).
 */
@Component
@Order(40)
class EndingOverdueTask implements M02DailyTask {

    static final String NAME = "ENDINGS";
    static final String CAUSE = "RESIDENCY_ENDING_OVERDUE";
    static final AuditAction MARKED = new AuditAction("M02_ENDING_OVERDUE_MARKED", false);

    private final ResidencyEndingStore store;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;

    EndingOverdueTask(
            ResidencyEndingStore store,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            ApplicationEventPublisher events) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.events = events;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int run(LocalDate day) {
        int changed = 0;
        for (ResidencyEnding listed : store.preparingBefore(day)) {
            residencyLock.lock(listed.residentId());
            ResidencyEnding ending = store.find(listed.id()).orElseThrow();
            if (!ending.preparing() || !ending.expectedEndDate().isBefore(day)) {
                continue;
            }
            LocalDate since = ending.expectedEndDate().plusDays(1);
            if (ending.overdueSince() == null) {
                store.setOverdueSince(ending.id(), since);
                auditLog.write(AuditLogEntry.builder()
                        .action(MARKED)
                        .entity(ResidencyEndingService.ENTITY_TYPE, ending.id())
                        .resident(ending.residentId())
                        .after(Map.of("overdueSince", since.toString()))
                        .build());
                events.publishEvent(new ResidencyDaysChanged(ending.residentId(), since, day, CAUSE));
            } else {
                events.publishEvent(new ResidencyDaysChanged(ending.residentId(), day, day, CAUSE));
            }
            changed++;
        }
        return changed;
    }
}
