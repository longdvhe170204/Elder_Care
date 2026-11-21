package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.api.events.ResidencyChangeConfirmationAnswered;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.admission.internal.change.domain.ChangeConfirmationRequest;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Người đại diện xem và trả lời đề nghị xác nhận thay đổi lưu trú trên ứng dụng (FR-043c; api-004 tag
 * {@code change-requests}). Người đại diện xét theo {@link RepresentativePort} tại lúc thao tác, như xác nhận người nhận đồ
 * của 013; người đại diện thấy nội dung kèm ảnh hưởng chi phí (D chọn 2026-11-20). Đồng ý thay cho xác nhận kèm bằng chứng;
 * không đồng ý bắt buộc lý do, yêu cầu vẫn ở Nháp để Hành chính sửa hoặc hủy.
 */
@Service
public class RepresentativeChangeConfirmationService {

    static final AuditAction ACCEPTED = new AuditAction("M02_CHANGE_CONFIRMATION_ACCEPTED", false);
    static final AuditAction DECLINED = new AuditAction("M02_CHANGE_CONFIRMATION_DECLINED", false);

    /** Một đề nghị đang chờ người đại diện, kèm nội dung yêu cầu. */
    public record PortalView(
            long id,
            long approvalRequestId,
            long residentId,
            String status,
            Instant sentAt,
            JsonNode changeTypes,
            JsonNode previousValue,
            JsonNode newValue,
            JsonNode costImpact,
            LocalDate requestedEffectiveDate,
            String reason) {}

    private final ChangeConfirmations confirmations;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final ObjectProvider<RepresentativePort> representatives;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    RepresentativeChangeConfirmationService(
            ChangeConfirmations confirmations,
            ObjectProvider<ApprovalRequestApi> approvals,
            ObjectProvider<RepresentativePort> representatives,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.confirmations = confirmations;
        this.approvals = approvals;
        this.representatives = representatives;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
    }

    /** Đề nghị đang chờ của những người cao tuổi mà người dùng hiện là người đại diện. */
    @Transactional(readOnly = true)
    public List<PortalView> pending() {
        long familyMemberId = familyMember();
        List<PortalView> views = new ArrayList<>();
        for (SimpleRecord<ChangeConfirmationRequest> r : confirmations.awaiting()) {
            if (r.residentId() != null && isRepresentative(familyMemberId, r.residentId())) {
                approvals.getObject().find(r.content().requestId()).ifPresent(request -> views.add(view(r, request)));
            }
        }
        return views;
    }

    @Transactional
    public void accept(long id) {
        answer(id, true, null);
    }

    @Transactional
    public void decline(long id, Reason reason) {
        if (reason == null || reason.value() == null || reason.value().isBlank()) {
            throw new InvalidValueException("Nhập lý do không đồng ý");
        }
        answer(id, false, reason.value().strip());
    }

    private void answer(long id, boolean accepted, String reason) {
        long familyMemberId = familyMember();
        SimpleRecord<ChangeConfirmationRequest> record = confirmations.lock(id);
        if (record.residentId() == null || !isRepresentative(familyMemberId, record.residentId())) {
            throw new PermissionDeniedException("Chỉ người đại diện hiện tại xác nhận thay đổi lưu trú");
        }
        if (!ChangeConfirmationRequest.AWAITING.equals(record.status())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONFIRMATION_NOT_AWAITING",
                    "Đề nghị này không còn chờ xác nhận (đã trả lời hoặc viện đã sửa nội dung)");
        }
        long requestId = record.content().requestId();
        ApprovalRequestView request = approvals.getObject().find(requestId).orElseThrow(NotFoundException::new);
        if (!"DRAFT".equals(request.status())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "REQUEST_NOT_DRAFT", "Yêu cầu thay đổi lưu trú không còn chờ xác nhận");
        }
        Instant now = time.now();
        confirmations.change(
                record,
                accepted ? ChangeConfirmationRequest.ACCEPTED : ChangeConfirmationRequest.DECLINED,
                record.content()
                        .answered(familyMemberId, currentUser.getObject().accountId(), now, reason));
        auditLog.write(AuditLogEntry.builder()
                .action(accepted ? ACCEPTED : DECLINED)
                .entity(ChangeConfirmationRequest.KIND.code(), id)
                .resident(record.residentId())
                .after(
                        reason == null
                                ? Map.of("approvalRequestId", requestId)
                                : Map.of("approvalRequestId", requestId, "declineReason", reason))
                .build());
        events.publishEvent(new ResidencyChangeConfirmationAnswered(requestId, record.residentId(), accepted, now));
    }

    private long familyMember() {
        return currentUser
                .getObject()
                .familyMemberId()
                .orElseThrow(() -> new PermissionDeniedException("Chỉ người đại diện dùng cổng người thân"));
    }

    private boolean isRepresentative(long familyMemberId, long residentId) {
        return representatives.getObject().isActiveRepresentative(familyMemberId, residentId, time.today());
    }

    private static PortalView view(SimpleRecord<ChangeConfirmationRequest> record, ApprovalRequestView request) {
        JsonNode content = request.content();
        return new PortalView(
                record.id(),
                request.id(),
                record.residentId(),
                record.status(),
                record.content().sentAt(),
                content.get("changeTypes"),
                content.get("previousValue"),
                content.get("newValue"),
                content.get("costImpact"),
                request.requestedEffectiveDate(),
                request.reason());
    }
}
