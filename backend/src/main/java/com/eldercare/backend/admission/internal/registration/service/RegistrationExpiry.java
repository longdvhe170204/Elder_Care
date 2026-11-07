package com.eldercare.backend.admission.internal.registration.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder.Kind;
import com.eldercare.backend.admission.internal.registration.domain.ObligationTrigger;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.persistence.ResidenceRegistrationStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chuyển Hết hạn theo giờ (FR-005, FR-006, BR-M02-12; quickstart Q13): Đăng ký tạm trú Đã xác nhận mà ngày hết hạn hiện hành
 * (theo đính chính) đã qua trước ngày {@code day} thì chuyển Hết hạn và báo Quản lý viện. Người chưa ở trạng thái cuối, chưa
 * có khai báo đang xử lý thì ghi nghĩa vụ mốc {@code EXPIRED} (mốc là đầu ngày sau ngày hết hạn, hạn = mốc + CFG-M02-12) và
 * nhắc Hành chính lập; không lập khai báo (Q-306). Khai báo gia hạn Đã xác nhận trước đó đã làm khai báo cũ Được thay thế.
 */
@Component
@Order(10)
public class RegistrationExpiry implements M02DailyTask {

    static final AuditAction EXPIRED = new AuditAction("M02_REGISTRATION_EXPIRED", false);

    private final ResidenceRegistrationStore registrations;
    private final RegistrationExpiryDates expiryDates;
    private final RegistrationObligations obligations;
    private final ResidentApi residents;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    RegistrationExpiry(
            ResidenceRegistrationStore registrations,
            RegistrationExpiryDates expiryDates,
            RegistrationObligations obligations,
            ResidentApi residents,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.registrations = registrations;
        this.expiryDates = expiryDates;
        this.obligations = obligations;
        this.residents = residents;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return "REG_EXPIRY";
    }

    /** Việc xét trạng thái tại ngày chạy nên ngày bị lỡ chỉ ghi bỏ qua; lần chạy đầu sau đó xử lý đủ (SC-002). */
    @Override
    public boolean latestDayOnly() {
        return true;
    }

    @Override
    public int run(LocalDate day) {
        return expire(day);
    }

    /** Chuyển Hết hạn các khai báo hết hạn trước ngày {@code day}; trả số khai báo vừa chuyển. */
    @Transactional
    public int expire(LocalDate day) {
        List<ResidenceRegistration> confirmed = registrations.confirmedTemporaryResidence();
        Map<Long, LocalDate> expiry = expiryDates.of(confirmed);
        Set<Long> inProgress = registrations.residentsWithInProgress();
        int expired = 0;
        for (ResidenceRegistration registration : confirmed) {
            LocalDate expiryDate = expiry.get(registration.id());
            if (expiryDate == null || !expiryDate.isBefore(day) || !registrations.markExpired(registration.id())) {
                continue;
            }
            long residentId = registration.residentId();
            Instant trigger =
                    expiryDate.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
            auditLog.write(AuditLogEntry.builder()
                    .action(EXPIRED)
                    .entity(RegistrationNames.ENTITY_TYPE, registration.id())
                    .resident(residentId)
                    .before(Map.of("status", RegistrationStatus.CONFIRMED.name()))
                    .after(Map.of("status", RegistrationStatus.EXPIRED.name(), "expiryDate", expiryDate.toString()))
                    .build());
            // Người đã Kết thúc lưu trú, Qua đời chỉ còn khai báo xóa: không báo Hết hạn, không nhắc lập khai báo
            boolean finalStatus = RegistrationRenewalReminders.FINAL.contains(residents.status(residentId));
            if (!finalStatus) {
                events.publishEvent(
                        new ResidenceRegistrationReminder(Kind.EXPIRED_MANAGER, residentId, day, time.now()));
            }
            if (!inProgress.contains(residentId) && !finalStatus) {
                obligations.record(residentId, ObligationTrigger.EXPIRED, trigger, registration.id());
            }
            expired++;
        }
        return expired;
    }
}
