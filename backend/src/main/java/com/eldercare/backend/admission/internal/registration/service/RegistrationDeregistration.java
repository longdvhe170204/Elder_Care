package com.eldercare.backend.admission.internal.registration.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder.Kind;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.ReminderType;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationReminderStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationResidentStore;
import com.eldercare.backend.admission.internal.registration.persistence.ResidenceRegistrationStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khai báo khi người cao tuổi sang trạng thái cuối (018 US4, T014; FR-005, FR-008, BR-M02-13; research Z-05). Tác động kèm
 * lệnh của 001 ({@link #finalStatusReached}, không chặn, SC-004): khai báo Cần khai báo, Đã nộp → Đã hủy, nghĩa vụ đang mở
 * hủy; có khai báo Đã xác nhận hoặc Hết hạn thì nhắc Hành chính khai báo xóa ngay. "Cần khai báo xóa" là số liệu tính lúc
 * hỏi ({@link #due}): người Kết thúc lưu trú hoặc Qua đời còn khai báo Đã xác nhận, Hết hạn; nhắc mỗi CFG-M02-09
 * ({@link #remind}). Không lập khai báo (Q-306), không xóa dòng.
 */
@Component
@Order(40)
public class RegistrationDeregistration implements M02DailyTask {

    static final AuditAction CANCELLED = new AuditAction("M02_REGISTRATION_CANCELLED", false);

    /** Trạng thái cuối có nghĩa vụ khai báo xóa (FR-008); Hủy tiếp nhận không có khai báo Đã xác nhận. */
    static final Set<ResidentStatus> ENDED = Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED);

    private final ResidenceRegistrationStore registrations;
    private final RegistrationObligationStore obligations;
    private final RegistrationResidentStore residentLock;
    private final RegistrationReminderStore reminders;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    RegistrationDeregistration(
            ResidenceRegistrationStore registrations,
            RegistrationObligationStore obligations,
            RegistrationResidentStore residentLock,
            RegistrationReminderStore reminders,
            ResidentApi residents,
            ParameterApi parameters,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.registrations = registrations;
        this.obligations = obligations;
        this.residentLock = residentLock;
        this.reminders = reminders;
        this.residents = residents;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
    }

    /**
     * Tác động của Kết thúc lưu trú, Ghi nhận qua đời, Hủy tiếp nhận, trong transaction của lệnh 001. Khóa người cao tuổi
     * của 018 trước nên lệnh khai báo đang chạy (ghi nộp, lập) xong rồi mới bị hủy, hoặc chạy sau và thấy Đã hủy. Gọi lại
     * không đổi gì thêm (câu lệnh có điều kiện trạng thái, khóa nhắc theo ngày).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void finalStatusReached(TransitionResult result) {
        long residentId = result.residentId();
        residentLock.lock(residentId);
        for (ResidenceRegistration registration : registrations.inProgressForUpdate(residentId)) {
            if (registrations.markCancelled(registration.id())) {
                auditLog.write(AuditLogEntry.builder()
                        .action(CANCELLED)
                        .entity(RegistrationNames.ENTITY_TYPE, registration.id())
                        .resident(residentId)
                        .before(Map.of("status", registration.status().name()))
                        .after(Map.of(
                                "status", RegistrationStatus.CANCELLED.name(),
                                "command", result.command().name()))
                        .build());
            }
        }
        obligations.cancelOpen(residentId, time.now());
        if (result.command() != StatusCommand.CANCEL_ADMISSION && registrations.hasConfirmedOrExpired(residentId)) {
            LocalDate today = time.today();
            if (reminders.claim(ReminderType.DEREGISTER, residentId, today)) {
                events.publishEvent(new ResidenceRegistrationReminder(Kind.DEREGISTER, residentId, today, time.now()));
            }
        }
    }

    @Override
    public String name() {
        return "REG_DEREGISTER";
    }

    /** Việc xét trạng thái tại ngày chạy nên ngày bị lỡ chỉ ghi bỏ qua; lần chạy đầu sau đó xử lý đủ (SC-002). */
    @Override
    public boolean latestDayOnly() {
        return true;
    }

    @Override
    public int run(LocalDate day) {
        return remind(day);
    }

    /** Nhắc của ngày {@code day} (mỗi CFG-M02-09 kể từ lần nhắc trước); trả số người được nhắc. */
    @Transactional
    public int remind(LocalDate day) {
        long everyDays = Math.max(
                1, parameters.read(ParameterCode.CFG_M02_09).asDuration().toDays());
        Set<Long> residentIds = new LinkedHashSet<>();
        due().forEach(registration -> residentIds.add(registration.residentId()));
        int reminded = 0;
        for (long residentId : residentIds) {
            Optional<LocalDate> last = reminders.lastOn(ReminderType.DEREGISTER, residentId);
            if (last.isPresent() && day.isBefore(last.get().plusDays(everyDays))) {
                continue;
            }
            if (reminders.claim(ReminderType.DEREGISTER, residentId, day)) {
                events.publishEvent(new ResidenceRegistrationReminder(Kind.DEREGISTER, residentId, day, time.now()));
                reminded++;
            }
        }
        return reminded;
    }

    /** Khai báo cần khai báo xóa: Đã xác nhận, Hết hạn của người Kết thúc lưu trú hoặc Qua đời (FR-008). */
    List<ResidenceRegistration> due() {
        Set<Long> ended = new HashSet<>(residents.byStatus(ENDED));
        return registrations.confirmedOrExpired().stream()
                .filter(registration -> ended.contains(registration.residentId()))
                .toList();
    }
}
