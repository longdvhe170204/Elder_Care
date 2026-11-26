package com.eldercare.backend.admission.internal.ending.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.api.events.ResidencyEndingRequestChanged;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.admission.internal.absence.service.AbsenceNames;
import com.eldercare.backend.admission.internal.ending.domain.EndingRequestContent;
import com.eldercare.backend.admission.internal.ending.persistence.ResidencyEndingStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu kết thúc lưu trú người đại diện gửi trên app (demo 2026-11-26, E làm, chờ B, chủ dự án duyệt; api-004 tag
 * {@code residency-ending}). Người đại diện xét theo {@link RepresentativePort} tại lúc thao tác như xác nhận thay đổi lưu
 * trú; mỗi người cao tuổi một yêu cầu Chờ xử lý; gửi, hủy báo Hành chính.
 */
@Service
public class EndingRequestService {

    static final AuditAction SUBMITTED = new AuditAction("M02_ENDING_REQUEST_SUBMITTED", false);
    static final AuditAction CANCELLED = new AuditAction("M02_ENDING_REQUEST_CANCELLED", false);

    private static final Set<ResidentStatus> CAN_END =
            Set.of(ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED);
    private static final Set<EndCase> FAMILY_CASES = Set.of(EndCase.CONTRACT_EXPIRED, EndCase.CONTRACT_TERMINATION);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<EndCase, String> CASE_TEXT =
            Map.of(EndCase.CONTRACT_EXPIRED, "hết hạn hợp đồng", EndCase.CONTRACT_TERMINATION, "chấm dứt hợp đồng");

    /** Yêu cầu kèm tên người cao tuổi, người thân để hiển thị. */
    public record View(SimpleRecord<EndingRequestContent> record, String residentName, String familyMemberName) {}

    private final SimpleRecordApi records;
    private final ResidencyEndingStore endings;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final AbsenceNames names;
    private final ObjectProvider<RepresentativePort> representatives;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    EndingRequestService(
            SimpleRecordApi records,
            ResidencyEndingStore endings,
            ResidencyLock residencyLock,
            ResidentApi residents,
            AbsenceNames names,
            ObjectProvider<RepresentativePort> representatives,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.records = records;
        this.endings = endings;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.names = names;
        this.representatives = representatives;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
    }

    @Transactional
    public View submit(long residentId, EndCase endCase, LocalDate expectedEndDate, String reason) {
        long familyMemberId = representativeOf(residentId);
        if (endCase == null || !FAMILY_CASES.contains(endCase)) {
            throw new InvalidValueException("Chọn hết hạn hoặc chấm dứt hợp đồng");
        }
        if (expectedEndDate == null || expectedEndDate.isBefore(time.today())) {
            throw new InvalidValueException("Ngày kết thúc mong muốn không được trước hôm nay");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidValueException("Nhập lý do");
        }
        if (!CAN_END.contains(residents.status(residentId))) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENT_NOT_IN_RESIDENCE",
                    "Người cao tuổi không còn đang lưu trú, không gửi được yêu cầu kết thúc");
        }
        residencyLock.lock(residentId);
        if (endings.lockPreparingOf(residentId).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENCY_ENDING_ALREADY_OPEN",
                    "Viện đã lập hồ sơ kết thúc lưu trú cho người cao tuổi này");
        }
        if (!records.listByResident(EndingRequestContent.KIND, residentId, Set.of(EndingRequestContent.PENDING))
                .isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ENDING_REQUEST_ALREADY_PENDING",
                    "Đã có yêu cầu kết thúc lưu trú đang chờ viện xử lý");
        }
        Instant now = time.now();
        long accountId = currentUser.getObject().accountId();
        SimpleRecord<EndingRequestContent> created = records.create(
                EndingRequestContent.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        null,
                        EndingRequestContent.PENDING,
                        EndingRequestContent.submitted(familyMemberId, endCase, expectedEndDate, reason.strip(), now),
                        null,
                        null,
                        null,
                        accountId,
                        false));
        auditLog.write(AuditLogEntry.builder()
                .action(SUBMITTED)
                .entity(EndingRequestContent.KIND.code(), created.id())
                .resident(residentId)
                .after(Map.of("endCase", endCase.name(), "expectedEndDate", expectedEndDate.toString()))
                .build());
        View view = view(created);
        events.publishEvent(new ResidencyEndingRequestChanged(
                created.id(),
                residentId,
                "SUBMITTED",
                view.familyMemberName() + " (người thân) gửi yêu cầu kết thúc lưu trú cho " + view.residentName()
                        + ": " + CASE_TEXT.get(endCase) + ", ngày " + expectedEndDate.format(DATE) + ". Lý do: "
                        + reason.strip(),
                now));
        return view;
    }

    /** Yêu cầu của một người cao tuổi, mới nhất trước (người đại diện). */
    @Transactional(readOnly = true)
    public List<View> forRepresentative(long residentId) {
        representativeOf(residentId);
        return records.listByResident(EndingRequestContent.KIND, residentId, EndingRequestContent.ALL).stream()
                .sorted(Comparator.comparingLong(SimpleRecord<EndingRequestContent>::id)
                        .reversed())
                .map(this::view)
                .toList();
    }

    @Transactional
    public View cancel(long id) {
        SimpleRecord<EndingRequestContent> record = records.lockForUpdate(EndingRequestContent.KIND, id);
        if (record.residentId() == null) throw new PermissionDeniedException("Không phải người đại diện hiện tại");
        representativeOf(record.residentId());
        requirePending(record);
        Instant now = time.now();
        SimpleRecord<EndingRequestContent> after = change(
                record,
                EndingRequestContent.CANCELLED,
                record.content().decided(currentUser.getObject().accountId(), now, null, null));
        auditLog.write(AuditLogEntry.builder()
                .action(CANCELLED)
                .entity(EndingRequestContent.KIND.code(), id)
                .resident(record.residentId())
                .build());
        View view = view(after);
        events.publishEvent(new ResidencyEndingRequestChanged(
                id,
                record.residentId(),
                "CANCELLED",
                view.familyMemberName() + " (người thân) đã hủy yêu cầu kết thúc lưu trú cho " + view.residentName()
                        + ".",
                now));
        return view;
    }

    static void requirePending(SimpleRecord<EndingRequestContent> record) {
        if (!EndingRequestContent.PENDING.equals(record.status())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ENDING_REQUEST_NOT_PENDING", "Yêu cầu không còn chờ xử lý");
        }
    }

    SimpleRecord<EndingRequestContent> change(
            SimpleRecord<EndingRequestContent> record, String status, EndingRequestContent content) {
        return records.change(
                        EndingRequestContent.KIND,
                        record.id(),
                        record.version(),
                        new RecordChange<>(status, content, record.effectiveTo(), record.uniqueKey(), false))
                .after();
    }

    View view(SimpleRecord<EndingRequestContent> record) {
        return new View(
                record,
                residents.identity(record.residentId()).fullName(),
                names.familyMember(record.content().familyMemberId()));
    }

    private long representativeOf(long residentId) {
        long familyMemberId = currentUser
                .getObject()
                .familyMemberId()
                .orElseThrow(() -> new PermissionDeniedException("Chỉ người đại diện dùng cổng người thân"));
        if (!representatives.getObject().isActiveRepresentative(familyMemberId, residentId, time.today())) {
            throw new PermissionDeniedException("Chỉ người đại diện hiện tại gửi yêu cầu kết thúc lưu trú");
        }
        return familyMemberId;
    }
}
