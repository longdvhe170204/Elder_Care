package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.administration.account.api.spi.StaffStatus;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestDetails;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.RepresentativeRequestNeedsCompletion;
import com.eldercare.backend.admission.api.events.ResidencyChangeConfirmationRequested;
import com.eldercare.backend.admission.api.events.ResidencyChangeNeedsConfirmation;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.change.domain.ChangeConfirmationRequest;
import com.eldercare.backend.admission.internal.change.domain.RepresentativeConfirmation;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeDigest;
import com.eldercare.backend.admission.internal.change.domain.ResidentOpinion;
import com.eldercare.backend.admission.internal.change.persistence.AddendumStore;
import com.eldercare.backend.admission.internal.change.persistence.ResidentOpinionStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractVersion;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.contract.service.ContractService;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lập và xem yêu cầu thay đổi lưu trú (FR-037 → FR-040, FR-043; api-004 tag {@code change-requests}). Yêu cầu là một yêu cầu
 * phê duyệt của 000, loại {@code M02.RESIDENCY_CHANGE}; lệnh này luôn lập ở Nháp, gửi duyệt, duyệt, từ chối, hủy qua 000.
 * Hệ thống chỉ điền giá trị trước và ảnh hưởng chi phí (FR-038), không tự lập yêu cầu (Q-306). Phụ lục do {@link
 * ResidencyChangeHandler} tạo khi yêu cầu được duyệt.
 */
@Service
public class ResidencyChangeService {

    /** Yêu cầu kèm các số liệu tính lúc đọc. */
    public record View(
            ApprovalRequestDetails details,
            ResidencyChangeContent content,
            boolean minorChange,
            Long addendumId,
            String requestedBy,
            boolean financeVisible,
            Target target,
            boolean confirmationRequired,
            Optional<RepresentativeConfirmation> confirmation,
            String confirmationRecordedBy,
            Optional<SimpleRecord<ChangeConfirmationRequest>> confirmationRequest,
            Optional<Addendum> addendum,
            Optional<com.eldercare.backend.resident.api.dto.AssessmentSummary> assessed,
            Optional<ResidentOpinion> currentOpinion,
            boolean currentOpinionOutdated,
            List<String> warnings) {}

    static final AuditAction CONFIRMATION_SENT = new AuditAction("M02_CHANGE_CONFIRMATION_SENT", false);

    private final ContractStore contracts;
    private final AddendumStore addenda;
    private final ResidencyChangeRules rules;
    private final ResidencyChangeHandler handler;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<AccountApi> accounts;
    private final ObjectProvider<PermissionApi> permissions;
    private final FileApi files;
    private final ChangeConfirmations confirmations;
    private final ObjectProvider<RepresentativePort> representatives;
    private final AuditLogApi auditLog;
    private final ResidencyLock residencyLock;
    private final ObjectProvider<StaffProfileApi> staff;
    private final ResidentOpinionStore opinions;
    private final ObjectProvider<ScopeApi> scopes;

    ResidencyChangeService(
            ResidencyLock residencyLock,
            ObjectProvider<StaffProfileApi> staff,
            ResidentOpinionStore opinions,
            ObjectProvider<ScopeApi> scopes,
            ChangeConfirmations confirmations,
            ObjectProvider<RepresentativePort> representatives,
            AuditLogApi auditLog,
            ContractStore contracts,
            AddendumStore addenda,
            ResidencyChangeRules rules,
            ResidencyChangeHandler handler,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<ApprovalRequestApi> approvals,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<PermissionApi> permissions,
            FileApi files) {
        this.confirmations = confirmations;
        this.residencyLock = residencyLock;
        this.staff = staff;
        this.opinions = opinions;
        this.scopes = scopes;
        this.representatives = representatives;
        this.auditLog = auditLog;
        this.files = files;
        this.contracts = contracts;
        this.addenda = addenda;
        this.rules = rules;
        this.handler = handler;
        this.events = events;
        this.time = time;
        this.approvals = approvals;
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.permissions = permissions;
    }

    /**
     * Hợp đồng của yêu cầu: {@code contractId} nếu có; không thì hợp đồng Hiệu lực của {@code residentId} (Bác sĩ không
     * xem được hợp đồng nên lập theo người cao tuổi, FR-039). Không có hợp đồng Hiệu lực thì 409 {@code CONTRACT_NOT_ACTIVE}.
     */
    public long contractFor(Long contractId, Long residentId) {
        if (contractId != null) {
            return contractId;
        }
        if (residentId == null) {
            throw new InvalidValueException("Cần contractId hoặc residentId");
        }
        return contracts.ofResident(residentId).stream()
                .filter(c -> c.status() == ContractStatus.ACTIVE)
                .map(Contract::id)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT,
                        "CONTRACT_NOT_ACTIVE",
                        "Người cao tuổi không có hợp đồng đang Hiệu lực (FR-037)"));
    }

    /** Đối tượng của hợp đồng để kiểm quyền; không có hợp đồng thì 404. */
    public Target targetOf(long contractId) {
        Contract contract = contracts.find(contractId).orElseThrow(NotFoundException::new);
        return ContractService.target(contract.id(), contract.residentId());
    }

    /**
     * Lập yêu cầu ở Nháp. {@code input} là {@code {contractId, changeTypes, newValue, reference?}}; ngày hiệu lực mong muốn
     * không sớm hơn ngày bắt đầu hợp đồng gốc (FR-040), hợp đồng phải Hiệu lực (FR-037).
     */
    @BusinessCommand(requiresReason = true)
    @RequiresPermission(function = Function.RESIDENCY_CHANGE_REQUEST, operation = Operation.CREATE)
    @Transactional
    public View create(Target target, JsonNode input, LocalDate requestedEffectiveDate, Reason reason) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(input);
        Contract contract = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
        ObjectNode content = withSystemValues(input, change, contract, requestedEffectiveDate);
        recordConfirmation(content, contract.residentId());
        ApprovalRequestView request = approvals
                .getObject()
                .create(new CreateRequest(ResidencyChangeHandler.TYPE, content, requestedEffectiveDate, reason, false));
        attachEvidence(content, null, contract.id());
        addenda.addAttributes(
                request.id(), contract.id(), contract.residentId(), change.changeTypes(), request.requesterId());
        handler.created(change, request.id());
        CurrentUser user = currentUser.getObject();
        if (user.type() == AccountType.FAMILY_MEMBER) {
            // FR-043i (Q-418): dấu phần thực chất lúc người đại diện gửi, để biết Hành chính có đổi phần thực chất
            // không
            opinions.savePortalDigest(
                    request.id(), ResidencyChangeDigest.of(content, requestedEffectiveDate), time.now());
            events.publishEvent(
                    new RepresentativeRequestNeedsCompletion(request.id(), contract.residentId(), time.now()));
        } else if (!mayConfirm(user)) {
            // Bác sĩ lập: Hành chính xác nhận với người đại diện rồi gửi duyệt (2026-11-20)
            events.publishEvent(new ResidencyChangeNeedsConfirmation(request.id(), contract.residentId(), time.now()));
        }
        return view(approvals.getObject().detail(request.id()));
    }

    /**
     * Sửa yêu cầu còn Nháp (T043, T047; FR-038, FR-039): cùng kiểm như lúc lập, điền lại giá trị trước và ảnh hưởng chi
     * phí, rồi sửa qua {@code ApprovalRequestApi.editDraft} trong transaction này (người sửa theo {@code mayActOnDraft},
     * Nháp, {@code If-Match}, nhật ký do 000 kiểm). Không xem được hoặc không phải loại này thì 404. Quyền lập như lệnh
     * lập, trên hợp đồng của nội dung mới ({@code target}).
     */
    @BusinessCommand(requiresReason = true)
    @RequiresPermission(function = Function.RESIDENCY_CHANGE_REQUEST, operation = Operation.CREATE)
    @Transactional
    public View edit(
            Target target,
            long approvalRequestId,
            String ifMatch,
            JsonNode input,
            LocalDate requestedEffectiveDate,
            Reason reason) {
        ApprovalRequestView current =
                approvals.getObject().detail(approvalRequestId).request();
        if (!ResidencyChangeHandler.TYPE.equals(current.type())) {
            throw new NotFoundException();
        }
        if (!handler.mayActOnDraft(currentUser.getObject(), current)) {
            throw new PermissionDeniedException("Chỉ người yêu cầu được thực hiện lệnh này");
        }
        ResidencyChangeContent change = ResidencyChangeContent.parse(input);
        Contract contract = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
        ObjectNode content = withSystemValues(input, change, contract, requestedEffectiveDate);
        recordConfirmation(content, contract.residentId());
        approvals.getObject().editDraft(approvalRequestId, ifMatch, content, requestedEffectiveDate, reason);
        // Nội dung đã đổi: lần gửi người đại diện đang chờ hoặc đã đồng ý không còn đúng (FR-043c)
        confirmations.supersede(approvalRequestId);
        attachEvidence(content, current.content(), contract.id());
        addenda.replaceAttributes(
                approvalRequestId,
                contract.id(),
                contract.residentId(),
                change.changeTypes(),
                currentUser.getObject().accountId());
        return view(approvals.getObject().detail(approvalRequestId));
    }

    /**
     * Gửi yêu cầu ở Nháp cho người đại diện xác nhận trên ứng dụng (FR-043c): chỉ Hành chính, Quản lý viện (403); yêu cầu
     * phải Nháp (409 {@code REQUEST_NOT_DRAFT}), cần xác nhận (409 {@code CONFIRMATION_NOT_REQUIRED} với thay đổi nhỏ, yêu
     * cầu người đại diện tự gửi), người cao tuổi có người đại diện (409 {@code NO_REPRESENTATIVE}); {@code If-Match} là
     * ETag của yêu cầu. Lần gửi trước còn chờ hoặc đã đồng ý thành Bị thay thế; người đại diện được báo.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCY_CHANGE_REQUEST, operation = Operation.CREATE)
    @Transactional
    public View sendForConfirmation(Target target, long approvalRequestId, String ifMatch) {
        ApprovalRequestDetails details = approvals.getObject().detail(approvalRequestId);
        ApprovalRequestView request = details.request();
        if (!ResidencyChangeHandler.TYPE.equals(request.type())) {
            throw new NotFoundException();
        }
        CurrentUser user = currentUser.getObject();
        if (!mayConfirm(user)) {
            throw new PermissionDeniedException("Chỉ Hành chính gửi người đại diện xác nhận");
        }
        VersionEtag.requireMatch(ifMatch, request.version(), null);
        if (!"DRAFT".equals(request.status())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "REQUEST_NOT_DRAFT", "Chỉ gửi người đại diện xác nhận khi yêu cầu còn Nháp");
        }
        if (!handler.confirmationRequired(request)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONFIRMATION_NOT_REQUIRED",
                    "Yêu cầu này không cần xác nhận với người đại diện (thay đổi nhỏ hoặc người đại diện tự gửi)");
        }
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        long residentId = contracts
                .find(change.contractId())
                .orElseThrow(NotFoundException::new)
                .residentId();
        if (representatives.getObject().activeRepresentative(residentId).isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NO_REPRESENTATIVE", "Người cao tuổi chưa có người đại diện để gửi xác nhận");
        }
        SimpleRecord<ChangeConfirmationRequest> sent =
                confirmations.send(approvalRequestId, residentId, user.accountId(), time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(CONFIRMATION_SENT)
                .entity(ChangeConfirmationRequest.KIND.code(), sent.id())
                .resident(residentId)
                .after(Map.of("approvalRequestId", approvalRequestId))
                .build());
        events.publishEvent(new ResidencyChangeConfirmationRequested(
                sent.id(),
                approvalRequestId,
                residentId,
                change.changeTypes().stream().map(Enum::name).collect(Collectors.toSet()),
                request.requestedEffectiveDate(),
                time.now()));
        return view(approvals.getObject().detail(approvalRequestId));
    }

    /** Thân lệnh Ghi ý kiến người cao tuổi (FR-043h). */
    public record OpinionInput(ResidentOpinion.Opinion opinion, ResidentOpinion.Source source, String note) {}

    /** Ý kiến kèm dấu "ghi trước khi nội dung thay đổi" (R18). */
    public record OpinionView(ResidentOpinion opinion, String recordedBy, boolean outdated) {}

    /**
     * Ghi ý kiến người cao tuổi về yêu cầu (FR-043h, Q-417): Hành chính, Bác sĩ; Điều dưỡng khi người cao tuổi trong phạm vi
     * phân công (403 với người khác); yêu cầu ở Nháp, Chờ duyệt (409 {@code REQUEST_NOT_OPEN}); ghi thêm, bản mới là hiện
     * hành; lưu dấu nội dung thực chất lúc ghi.
     */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public List<OpinionView> recordOpinion(long approvalRequestId, OpinionInput input) {
        ApprovalRequestView request = opinionTarget(approvalRequestId, true);
        if (input == null || input.opinion() == null || input.source() == null) {
            throw new InvalidValueException("Cần ý kiến và nguồn của ý kiến");
        }
        String note = input.note() == null || input.note().isBlank()
                ? null
                : input.note().strip();
        if (note != null && note.length() > ResidentOpinion.NOTE_MAX) {
            throw new InvalidValueException("Ghi chú tối đa " + ResidentOpinion.NOTE_MAX + " ký tự");
        }
        if (!("DRAFT".equals(request.status()) || "PENDING_APPROVAL".equals(request.status()))) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "REQUEST_NOT_OPEN",
                    "Chỉ ghi ý kiến người cao tuổi khi yêu cầu còn Nháp hoặc Chờ duyệt");
        }
        opinions.insert(
                approvalRequestId,
                request.target().residentId(),
                input.opinion(),
                input.source(),
                note,
                ResidencyChangeDigest.of(request.content(), request.requestedEffectiveDate()),
                currentUser.getObject().accountId(),
                time.now());
        return opinionViews(request);
    }

    /**
     * Lịch sử ý kiến người cao tuổi của yêu cầu, mới nhất trước (FR-043h); người ghi được và Quản lý viện (người duyệt cần
     * xem ý kiến khi quyết định) xem được.
     */
    @Transactional(readOnly = true)
    public List<OpinionView> opinions(long approvalRequestId) {
        return opinionViews(opinionTarget(approvalRequestId, false));
    }

    private List<OpinionView> opinionViews(ApprovalRequestView request) {
        return opinions.ofRequest(request.id()).stream()
                .map(o -> new OpinionView(
                        o, accounts.getObject().fullName(o.recordedBy()).orElse(null), handler.outdated(request, o)))
                .toList();
    }

    /**
     * Yêu cầu để ghi, xem ý kiến: Điều dưỡng không xem được yêu cầu phê duyệt (000 FR-042) nên đọc thẳng, quyền xét theo vai
     * trò và phạm vi người cao tuổi. Ghi: Hành chính, Bác sĩ, Điều dưỡng phụ trách (FR-043h); xem: thêm Quản lý viện. Không
     * phải loại này thì 404.
     */
    private ApprovalRequestView opinionTarget(long approvalRequestId, boolean write) {
        ApprovalRequestView request =
                approvals.getObject().find(approvalRequestId).orElseThrow(NotFoundException::new);
        if (!ResidencyChangeHandler.TYPE.equals(request.type())
                || request.target().residentId() == null) {
            throw new NotFoundException();
        }
        CurrentUser user = currentUser.getObject();
        boolean allowed = user.type() != AccountType.FAMILY_MEMBER
                && (user.roles().contains(Role.OFFICE_STAFF)
                        || (!write && user.roles().contains(Role.NURSING_HOME_MANAGER))
                        || user.roles().contains(Role.DOCTOR)
                        || (user.roles().contains(Role.NURSING_STAFF)
                                && scopes.getObject().inScope(user, request.target())));
        if (!allowed) {
            throw new PermissionDeniedException(
                    write
                            ? "Chỉ Hành chính, Bác sĩ, Điều dưỡng phụ trách ghi ý kiến người cao tuổi"
                            : "Chỉ Hành chính, Bác sĩ, Điều dưỡng phụ trách, Quản lý viện xem ý kiến người cao tuổi");
        }
        return request;
    }

    /** Thân lệnh Ghi nhận phụ lục đã ký (FR-043d). */
    public record SignatureInput(
            LocalDate signedDate,
            Long signedByFamilyMemberId,
            Long facilitySignatoryStaffId,
            Long fileId,
            String note) {}

    /**
     * Ghi nhận phụ lục đã ký (FR-043d, Q-420): Hành chính, Quản lý viện; yêu cầu Đã duyệt, phụ lục Chờ ký (409 {@code
     * ADDENDUM_NOT_PENDING_SIGNATURE}); ngày ký không sau hôm nay, không trước ngày duyệt (422 {@code SIGNED_DATE_INVALID});
     * người ký là người đại diện hiệu lực tại ngày ký (409 {@code SIGNER_NOT_REPRESENTATIVE}); người ký phía viện (nếu có)
     * là nhân viên đang làm việc (422 {@code FACILITY_SIGNATORY_INVALID}); tệp gắn vào hợp đồng như bản scan hợp đồng.
     * Phụ lục chuyển Chờ hiệu lực; ký muộn hơn ngày hiệu lực đang có thì dời ngày hiệu lực của yêu cầu, phụ lục và giữ
     * giường. {@code If-Match} là ETag của yêu cầu.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCY_CHANGE_REQUEST, operation = Operation.CREATE)
    @Transactional
    public View recordSignature(Target target, long approvalRequestId, String ifMatch, SignatureInput input) {
        CurrentUser user = currentUser.getObject();
        if (!mayConfirm(user)) {
            throw new PermissionDeniedException("Chỉ Hành chính, Quản lý viện ghi nhận phụ lục đã ký");
        }
        if (input == null
                || input.signedDate() == null
                || input.signedByFamilyMemberId() == null
                || input.fileId() == null) {
            throw new InvalidValueException("Cần ngày ký, người đại diện ký và bản scan phụ lục đã ký");
        }
        ApprovalRequestView request =
                approvals.getObject().detail(approvalRequestId).request();
        if (!ResidencyChangeHandler.TYPE.equals(request.type())) {
            throw new NotFoundException();
        }
        long residentId = request.target().residentId();
        residencyLock.lock(residentId);
        VersionEtag.requireMatch(ifMatch, request.version(), null);
        Addendum addendum = addenda.lockByRequest(approvalRequestId).orElse(null);
        if (!"APPROVED".equals(request.status())
                || addendum == null
                || addendum.status() != Addendum.Status.PENDING_SIGNATURE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ADDENDUM_NOT_PENDING_SIGNATURE",
                    "Phụ lục của yêu cầu này không ở Chờ ký; chỉ ghi nhận bản đã ký một lần, sau khi yêu cầu được duyệt");
        }
        LocalDate approvedOn = request.decidedAt() == null
                ? addendum.createdAt().atZone(BusinessTime.ZONE).toLocalDate()
                : request.decidedAt().atZone(BusinessTime.ZONE).toLocalDate();
        LocalDate signed = input.signedDate();
        if (signed.isAfter(time.today()) || signed.isBefore(approvedOn)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "SIGNED_DATE_INVALID",
                    "Ngày ký phải từ ngày duyệt " + day(approvedOn) + " tới hôm nay");
        }
        if (!representatives.getObject().isActiveRepresentative(input.signedByFamilyMemberId(), residentId, signed)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SIGNER_NOT_REPRESENTATIVE",
                    "Người ký phải là người đại diện hiện tại của người cao tuổi vào ngày ký");
        }
        if (input.facilitySignatoryStaffId() != null && !working(input.facilitySignatoryStaffId(), signed)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "FACILITY_SIGNATORY_INVALID",
                    "Người ký phía viện phải là nhân viên đang làm việc");
        }
        files.attach(input.fileId(), ContractService.SCAN_OWNER_TYPE, addendum.contractId());
        addenda.markSigned(
                addendum.id(),
                signed,
                input.fileId(),
                input.signedByFamilyMemberId(),
                input.facilitySignatoryStaffId(),
                user.accountId(),
                time.now(),
                input.note());
        LocalDate before = request.actualEffectiveDate();
        if (signed.isAfter(before)) {
            // Ngày hiệu lực thực tế là ngày muộn nhất trong ngày mong muốn, ngày duyệt, ngày ký; không tính lùi (Q-420)
            approvals
                    .getObject()
                    .postponeEffectiveDate(approvalRequestId, signed, new Reason("Ký phụ lục ngày " + day(signed)));
            addenda.postponeEffective(addendum.id(), signed);
            handler.moveReservation(request, signed);
        }
        Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("status", Addendum.Status.PENDING_EFFECTIVE.name());
        after.put("signedDate", signed.toString());
        after.put("signedByFamilyMemberId", input.signedByFamilyMemberId());
        after.put("effectiveDate", (signed.isAfter(before) ? signed : before).toString());
        if (input.facilitySignatoryStaffId() != null) {
            after.put("facilitySignatoryStaffId", input.facilitySignatoryStaffId());
        }
        auditLog.write(AuditLogEntry.builder()
                .action(ResidencyChangeHandler.ADDENDUM_SIGNED)
                .entity(ResidencyChangeHandler.ADDENDUM_ENTITY, addendum.id())
                .resident(residentId)
                .before(Map.of("status", Addendum.Status.PENDING_SIGNATURE.name()))
                .after(after)
                .build());
        return view(approvals.getObject().detail(approvalRequestId));
    }

    /**
     * Hồ sơ nhân viên có và đang làm việc. Kiểm tồn tại bằng {@code fullName} trước: {@code status} ném 404 trong
     * transaction của 008, sẽ đánh dấu rollback cả lệnh.
     */
    private boolean working(long staffId, LocalDate date) {
        StaffProfileApi profiles = staff.getObject();
        return profiles.fullName(staffId).isPresent() && profiles.status(staffId, date) == StaffStatus.WORKING;
    }

    private static String day(LocalDate date) {
        return "%02d/%02d/%d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    /**
     * Hợp đồng phải Hiệu lực (FR-037), ngày hiệu lực mong muốn không sớm hơn ngày bắt đầu hợp đồng gốc (FR-040); trả nội
     * dung kèm giá trị trước và ảnh hưởng chi phí do hệ thống điền (FR-038).
     */
    private ObjectNode withSystemValues(
            JsonNode input, ResidencyChangeContent change, Contract contract, LocalDate requestedEffectiveDate) {
        if (contract.status() != ContractStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONTRACT_NOT_ACTIVE",
                    "Yêu cầu điều chỉnh hợp đồng cần hợp đồng đang Hiệu lực (FR-037)");
        }
        if (requestedEffectiveDate == null) {
            throw new InvalidValueException("requestedEffectiveDate là ngày bắt buộc");
        }
        if (requestedEffectiveDate.isBefore(contract.content().startDate())) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "EFFECTIVE_DATE_BEFORE_CONTRACT_START",
                    "Ngày hiệu lực mong muốn sớm hơn ngày bắt đầu hợp đồng gốc (FR-040)");
        }
        ContractVersion base = handler.base(contract.id())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "CONTRACT_NOT_ACTIVE", "Hợp đồng chưa có nội dung đã ký"));
        LocalDate effective = requestedEffectiveDate.isAfter(time.today()) ? requestedEffectiveDate : time.today();
        ObjectNode content = ((ObjectNode) input.deepCopy());
        content.set("previousValue", rules.previousValue(base.content(), change));
        content.set("costImpact", rules.costImpact(base.content(), change, effective));
        return content;
    }

    /**
     * Xác nhận với người đại diện (2026-11-20): chỉ Hành chính, Quản lý viện ghi (403 với người khác); ngày xác nhận không
     * sau hôm nay (422 {@code CONFIRMATION_DATE_IN_FUTURE}); hệ thống ghi người lưu và thời điểm. Sửa nháp không gửi lại
     * xác nhận thì xác nhận cũ bị bỏ, vì nội dung đã đổi phải xác nhận lại.
     */
    private void recordConfirmation(ObjectNode content, long residentId) {
        Optional<RepresentativeConfirmation> confirmation = RepresentativeConfirmation.of(content);
        if (confirmation.isEmpty()) {
            return;
        }
        CurrentUser user = currentUser.getObject();
        if (!mayConfirm(user)) {
            throw new PermissionDeniedException("Chỉ Hành chính ghi xác nhận với người đại diện");
        }
        if (confirmation.get().confirmedOn().isAfter(time.today())) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "CONFIRMATION_DATE_IN_FUTURE",
                    "Ngày xác nhận với người đại diện không được sau hôm nay");
        }
        Long confirmer = confirmation.get().representativeFamilyMemberId();
        if (confirmer == null) {
            throw new InvalidValueException("Cần chọn người đại diện đã xác nhận (Q-415)");
        }
        if (!representatives
                .getObject()
                .isActiveRepresentative(
                        confirmer, residentId, confirmation.get().confirmedOn())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONFIRMER_NOT_REPRESENTATIVE",
                    "Người đã xác nhận phải là người đại diện hiện tại của người cao tuổi vào ngày xác nhận");
        }
        content.set(
                RepresentativeConfirmation.FIELD,
                confirmation.get().recordedBy(user.accountId(), time.now()).toJson());
    }

    private static boolean mayConfirm(CurrentUser user) {
        return user.type() != AccountType.FAMILY_MEMBER
                && (user.roles().contains(Role.OFFICE_STAFF) || user.roles().contains(Role.NURSING_HOME_MANAGER));
    }

    /**
     * Tệp bằng chứng xác nhận gắn vào hợp đồng như bản scan hợp đồng (quyền tải theo quyền xem hợp đồng); tệp chỉ gắn một
     * lần nên sửa nháp giữ tệp cũ thì không gắn lại.
     */
    private void attachEvidence(JsonNode content, JsonNode previous, long contractId) {
        Optional<Long> fileId = RepresentativeConfirmation.of(content).map(RepresentativeConfirmation::evidenceFileId);
        if (fileId.isEmpty()) {
            return;
        }
        boolean attached = previous != null
                && RepresentativeConfirmation.of(previous)
                        .map(c -> c.evidenceFileId() == fileId.get())
                        .orElse(false);
        if (!attached) {
            files.attach(fileId.get(), ContractService.SCAN_OWNER_TYPE, contractId);
        }
    }

    /** Xem một yêu cầu; không phải loại này hoặc không xem được thì 404. */
    @Transactional(readOnly = true)
    public View get(long approvalRequestId) {
        ApprovalRequestDetails details = approvals.getObject().detail(approvalRequestId);
        if (!ResidencyChangeHandler.TYPE.equals(details.request().type())) {
            throw new NotFoundException();
        }
        return view(details);
    }

    /**
     * Danh sách yêu cầu lọc theo hợp đồng, trạng thái của yêu cầu phê duyệt ("Đổi api" 004, A duyệt 2026-11-07), mới nhất
     * trước. Nguồn là chỉ mục thuộc tính (FR-046); yêu cầu người dùng không xem được ({@code detail} 404) bị bỏ qua. Không
     * mở transaction bao ngoài: {@code detail} 404 trong transaction chung đánh dấu rollback-only, cả danh sách lỗi 500 (người
     * thân xem lịch sử yêu cầu, FR-039).
     */
    public List<View> list(Long contractId, Set<String> statuses) {
        Stream<Long> ids = contractId == null
                ? addenda.allRequests().stream()
                : contracts
                        .find(contractId)
                        .map(c -> addenda.requestsOf(c.residentId()).stream())
                        .orElseGet(Stream::empty)
                        .filter(id -> addenda.contractOf(id)
                                .filter(contractId::equals)
                                .isPresent());
        List<View> views = new ArrayList<>();
        ids.forEach(id -> {
            try {
                ApprovalRequestDetails details = approvals.getObject().detail(id);
                if (ResidencyChangeHandler.TYPE.equals(details.request().type())
                        && (statuses == null
                                || statuses.isEmpty()
                                || statuses.contains(details.request().status()))) {
                    views.add(view(details));
                }
            } catch (NotFoundException hidden) {
                // không xem được thì không liệt kê (FR-042 của 000)
            }
        });
        return views;
    }

    /** Phụ lục của hợp đồng (FR-047), mới theo ngày hiệu lực. */
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Addendum> addendaOf(Target target, long contractId) {
        return addenda.ofContract(contractId);
    }

    private View view(ApprovalRequestDetails details) {
        ApprovalRequestView request = details.request();
        ResidencyChangeContent content = ResidencyChangeContent.parse(request.content());
        Optional<Addendum> addendum = addenda.findByRequest(request.id());
        // Xác nhận bị bỏ khi người duyệt Trả về nháp thì không hiện nữa (Q-419)
        Optional<java.time.Instant> voided = confirmations.voidedAt(request.id());
        Optional<ResidentOpinion> currentOpinion = opinions.current(request.id());
        Optional<RepresentativeConfirmation> confirmation = RepresentativeConfirmation.of(request.content())
                .filter(c -> voided.isEmpty()
                        || (c.recordedAt() != null && c.recordedAt().isAfter(voided.get())));
        return new View(
                details,
                content,
                handler.isMinor(request),
                addendum.map(Addendum::id).orElse(null),
                accounts.getObject().fullName(request.requesterId()).orElse(null),
                financeVisible(request.target()),
                request.target(),
                handler.confirmationRequired(request),
                confirmation,
                confirmation
                        .map(RepresentativeConfirmation::recordedBy)
                        .flatMap(id -> accounts.getObject().fullName(id))
                        .orElse(null),
                confirmations.latest(request.id()),
                addendum,
                request.target() == null || request.target().residentId() == null
                        ? Optional.empty()
                        : handler.assessedLevel(request.target().residentId()),
                currentOpinion,
                currentOpinion.map(o -> handler.outdated(request, o)).orElse(false),
                handler.warnings(request));
    }

    /**
     * "Ảnh hưởng chi phí" là nội dung tài chính (FR-028): chỉ người xem được phần tài chính của hợp đồng thấy. Nhóm {@code
     * FINANCE} của 002 không thu hẹp vai trò nào ({@code field_visibility_rule}), nên quyền xem hợp đồng làm căn cứ.
     */
    private boolean financeVisible(Target target) {
        return permissions
                .getObject()
                .hasPermission(
                        currentUser.getObject(),
                        new PermissionKey(Function.ADMISSION_CONTRACT_DEPOSIT, Operation.VIEW),
                        target)
                .allowed();
    }
}
