package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.RepresentativeRequestNeedsCompletion;
import com.cambae.backend.m02.internal.change.domain.Addendum;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent;
import com.cambae.backend.m02.internal.change.persistence.AddendumStore;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractVersion;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.contract.service.ContractService;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestDetails;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.CreateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        ApprovalRequestView request = approvals
                .getObject()
                .create(new CreateRequest(ResidencyChangeHandler.TYPE, content, requestedEffectiveDate, reason, false));
        addenda.addAttributes(request.id(), contract.id(), contract.residentId(), change.changeTypes());
        if (currentUser.getObject().type() == AccountType.FAMILY_MEMBER) {
            events.publishEvent(
                    new RepresentativeRequestNeedsCompletion(request.id(), contract.residentId(), time.now()));
        }
        return view(approvals.getObject().detail(request.id()));
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
