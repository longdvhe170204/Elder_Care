package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestDetails;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.RepresentativeRequestNeedsCompletion;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent;
import com.eldercare.backend.admission.internal.change.persistence.AddendumStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractVersion;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.contract.service.ContractService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            Target target) {}

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

    ResidencyChangeService(
            ContractStore contracts,
            AddendumStore addenda,
            ResidencyChangeRules rules,
            ResidencyChangeHandler handler,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<ApprovalRequestApi> approvals,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<PermissionApi> permissions) {
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
        ApprovalRequestView request = approvals
                .getObject()
                .create(new CreateRequest(ResidencyChangeHandler.TYPE, content, requestedEffectiveDate, reason, false));
        addenda.addAttributes(
                request.id(), contract.id(), contract.residentId(), change.changeTypes(), request.requesterId());
        handler.created(change, request.id());
        if (currentUser.getObject().type() == AccountType.FAMILY_MEMBER) {
            events.publishEvent(
                    new RepresentativeRequestNeedsCompletion(request.id(), contract.residentId(), time.now()));
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
        approvals.getObject().editDraft(approvalRequestId, ifMatch, content, requestedEffectiveDate, reason);
        addenda.replaceAttributes(
                approvalRequestId,
                contract.id(),
                contract.residentId(),
                change.changeTypes(),
                currentUser.getObject().accountId());
        return view(approvals.getObject().detail(approvalRequestId));
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
                    "Yêu cầu thay đổi lưu trú cần hợp đồng đang Hiệu lực (FR-037)");
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
     * trước. Nguồn là chỉ mục thuộc tính (FR-046); yêu cầu người dùng không xem được ({@code detail} 404) bị bỏ qua.
     */
    @Transactional(readOnly = true)
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
        return new View(
                details,
                content,
                handler.isMinor(request),
                addendum.map(Addendum::id).orElse(null),
                accounts.getObject().fullName(request.requesterId()).orElse(null),
                financeVisible(request.target()),
                request.target());
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
