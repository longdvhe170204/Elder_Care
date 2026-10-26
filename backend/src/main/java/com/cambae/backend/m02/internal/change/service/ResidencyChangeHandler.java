package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.UnitPriceApi;
import com.cambae.backend.m02.api.dto.ServiceView;
import com.cambae.backend.m02.api.dto.UnitPriceView;
import com.cambae.backend.m02.api.events.AddendumApplied;
import com.cambae.backend.m02.api.events.DepositAmountChanged;
import com.cambae.backend.m02.api.spi.RepresentativePort;
import com.cambae.backend.m02.internal.change.domain.Addendum;
import com.cambae.backend.m02.internal.change.domain.ChangeType;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent;
import com.cambae.backend.m02.internal.change.persistence.AddendumStore;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractVersion;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.contract.service.ContractService;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.approval.api.ExtendedTransitions;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.api.spi.RequestTypeHandler;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu {@code M02.RESIDENCY_CHANGE} (FR-037 → FR-047, DBR-07; research T-06): thay đổi lưu trú qua yêu cầu có duyệt
 * thành phụ lục. Duyệt tạo phụ lục Chờ hiệu lực và khóa thuộc tính (FR-046); tới ngày hiệu lực {@link #apply} thêm phiên
 * bản nội dung, đổi mức chăm sóc qua 001, đổi ngày kết thúc và phát sự kiện trong một transaction (FR-042). Quản lý viện
 * duyệt mọi yêu cầu, Hành chính chỉ duyệt thay đổi nhỏ (FR-043, {@link IsMinorChange}); đổi mức chăm sóc của 001 chuyển yêu
 * cầu sang {@code SUPERSEDED} qua chuyển mở rộng (FR-036a).
 */
@Component
class ResidencyChangeHandler implements RequestTypeHandler {

    static final String TYPE = "M02.RESIDENCY_CHANGE";
    static final String ADDENDUM_ENTITY = "M02.ADDENDUM";
    static final String SUPERSEDE = "SUPERSEDE";
    static final String SUPERSEDED = "SUPERSEDED";
    static final AuditAction ADDENDUM_CREATED = new AuditAction("M02_ADDENDUM_CREATED", false);
    static final AuditAction ADDENDUM_APPLIED = new AuditAction("M02_ADDENDUM_APPLIED", false);
    static final AuditAction ADDENDUM_NOT_APPLIED = new AuditAction("M02_ADDENDUM_NOT_APPLIED", false);

    private final ContractStore contracts;
    private final AddendumStore addenda;
    private final ResidencyChangeRules rules;
    private final ResidencyLock residencyLock;
    private final AddendumClosing closing;
    private final CareLevelApi careLevels;
    private final UnitPriceApi prices;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<RepresentativePort> representatives;

    ResidencyChangeHandler(
            ContractStore contracts,
            AddendumStore addenda,
            ResidencyChangeRules rules,
            ResidencyLock residencyLock,
            AddendumClosing closing,
            CareLevelApi careLevels,
            UnitPriceApi prices,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<RepresentativePort> representatives) {
        this.contracts = contracts;
        this.addenda = addenda;
        this.rules = rules;
        this.residencyLock = residencyLock;
        this.closing = closing;
        this.careLevels = careLevels;
        this.prices = prices;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
        this.representatives = representatives;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    /**
     * Hình dạng nội dung (422) và ai được lập loại nào (FR-039): Bác sĩ chỉ loại đổi mức chăm sóc; người thân phải là
     * người đại diện hiệu lực của người cao tuổi.
     */
    @Override
    public void validateContent(JsonNode content) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(content);
        CurrentUser user = currentUser.getObject();
        if (user.roles().contains(Role.DOCTOR)
                && !user.roles().contains(Role.OFFICE_STAFF)
                && !change.changeTypes().equals(Set.of(ChangeType.CARE_LEVEL))) {
            throw new PermissionDeniedException("Bác sĩ chỉ lập yêu cầu đổi mức chăm sóc (FR-039)");
        }
        if (user.type() == AccountType.FAMILY_MEMBER) {
            Contract contract = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
            RepresentativePort port = representatives.getIfAvailable();
            boolean representative = port != null
                    && user.familyMemberId()
                            .map(id -> port.isActiveRepresentative(id, contract.residentId(), time.today()))
                            .orElse(false);
            if (!representative) {
                throw new PermissionDeniedException("Chỉ người đại diện hiệu lực lập yêu cầu qua cổng (FR-039)");
            }
        }
    }

    /** Hành chính, Bác sĩ, người đại diện lập (Phụ lục 27 dòng "Yêu cầu thay đổi lưu trú", ô T). */
    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.RESIDENCY_CHANGE_REQUEST, Operation.CREATE);
    }

    /** Quản lý viện duyệt mọi yêu cầu; Hành chính chỉ thay đổi nhỏ (ô D, chú thích 42) qua {@link #approverRoleRestriction}. */
    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.RESIDENCY_CHANGE_REQUEST, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        long contractId = ResidencyChangeContent.parse(content).contractId();
        return contracts
                .find(contractId)
                .map(contract -> ContractService.target(contract.id(), contract.residentId()))
                .orElse(new Target(ContractService.ENTITY_TYPE, String.valueOf(contractId), null, null));
    }

    /** Tính lại "thay đổi nhỏ" trên hợp đồng hiện hành ở mỗi lần xét (FR-043, FR-034a). */
    @Override
    public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return isMinor(request) ? Optional.empty() : Optional.of(Set.of(Role.NURSING_HOME_MANAGER));
    }

    boolean isMinor(ApprovalRequestView request) {
        return isMinor(request.content());
    }

    boolean isMinor(JsonNode content) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(content);
        return base(change.contractId())
                .map(base -> IsMinorChange.test(change, content.get("costImpact"), base.content()))
                .orElse(false);
    }

    /** FR-041, FR-046, FR-022, FR-040 khi duyệt; vi phạm thì 409 liệt kê đủ, yêu cầu giữ Chờ duyệt. */
    @Override
    public List<UnmetCondition> checkOnApprove(ApprovalRequestView request) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        Contract contract = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
        Optional<ContractVersion> base = base(change.contractId());
        if (base.isEmpty()) {
            return List.of(new UnmetCondition("CONTRACT_NOT_ACTIVE", "Hợp đồng chưa có nội dung đã ký"));
        }
        return rules.check(contract, base.get().content(), change, effectiveOn(request), request.id());
    }

    /** DBR-07: tạo phụ lục Chờ hiệu lực, khóa thuộc tính (FR-046); đổi giường theo lịch của 003 chờ giao diện của 003. */
    @Override
    public void onApprove(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        Contract found = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        Contract contract = contracts.lock(change.contractId()).orElseThrow(NotFoundException::new);
        ContractVersion base = base(contract.id()).orElseThrow();
        AddendumContent.Built built =
                AddendumContent.build(base.content(), change, new DiffPricing(actualEffectiveDate));
        long addendumId = addenda.insert(
                contract.id(),
                request.id(),
                change.changeTypes(),
                built.before(),
                built.after(),
                actualEffectiveDate,
                time.now());
        addenda.addAttributes(request.id(), contract.id(), contract.residentId(), change.changeTypes());
        try {
            addenda.setPending(request.id(), true);
        } catch (DuplicateKeyException e) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "DUPLICATE_PENDING_ATTRIBUTE", "Đã có yêu cầu Đã duyệt chờ hiệu lực cùng thuộc tính (FR-046)")));
        }
        auditLog.write(AuditLogEntry.builder()
                .action(ADDENDUM_CREATED)
                .entity(ADDENDUM_ENTITY, addendumId)
                .resident(contract.residentId())
                .after(Map.of(
                        "contractId", contract.id(),
                        "changeTypes", names(change.changeTypes()),
                        "effectiveDate", actualEffectiveDate.toString()))
                .basis(new Basis(null, request.id(), contract.id(), null))
                .build());
    }

    /**
     * FR-045: hợp đồng gốc còn Hiệu lực, người cao tuổi chưa ở trạng thái cuối, điều kiện FR-041 còn thỏa. Không thỏa thì
     * yêu cầu chuyển Áp dụng không thành ngay sau hàm này, cùng transaction, nên đóng phụ lục tại đây (phụ lục Không áp dụng,
     * thuộc tính được nhả); module không nghe {@code RequestApplyFailed} được (M13ArchTest).
     */
    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        List<UnmetCondition> unmet = applyConditions(request);
        if (!unmet.isEmpty()) {
            long residentId = request.target().residentId();
            residencyLock.lock(residentId);
            closing.close(request.id(), residentId, new Basis(null, request.id(), null, null));
        }
        return unmet;
    }

    private List<UnmetCondition> applyConditions(ApprovalRequestView request) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        Contract contract = contracts.find(change.contractId()).orElse(null);
        if (contract == null) {
            return List.of(new UnmetCondition("CONTRACT_NOT_ACTIVE", "Không còn hợp đồng gốc"));
        }
        Optional<Addendum> addendum = addenda.findByRequest(request.id());
        if (addendum.isEmpty() || addendum.get().status() != Addendum.Status.PENDING_EFFECTIVE) {
            return List.of(new UnmetCondition("ADDENDUM_NOT_PENDING", "Phụ lục của yêu cầu không ở Chờ hiệu lực"));
        }
        Optional<ContractVersion> base = base(change.contractId());
        if (base.isEmpty()) {
            return List.of(new UnmetCondition("CONTRACT_NOT_ACTIVE", "Hợp đồng chưa có nội dung đã ký"));
        }
        return rules.check(contract, base.get().content(), change, request.actualEffectiveDate(), null);
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate effective) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        Contract found = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        Contract contract = contracts.lock(change.contractId()).orElseThrow(NotFoundException::new);
        Addendum addendum = addenda.lockByRequest(request.id()).orElseThrow();
        ContractVersion base = base(contract.id()).orElseThrow();
        AddendumContent.Built built = AddendumContent.build(base.content(), change, new AppliedPricing(effective));
        long versionId = contracts.appendVersion(
                contract.id(), base.sequenceNo() + 1, effective, built.content(), addendum.id());
        if (change.has(ChangeType.CARE_LEVEL)) {
            careLevels.applyChange(contract.residentId(), change.careLevel(), effective, request.id());
        }
        LocalDate newEnd = built.content().hasNonNull("endDate")
                ? LocalDate.parse(built.content().get("endDate").asString())
                : null;
        if (newEnd != null && !newEnd.equals(contract.currentEndDate())) {
            boolean extended = contract.currentEndDate() == null || newEnd.isAfter(contract.currentEndDate());
            contracts.setCurrentEndDate(contract.id(), newEnd, extended ? null : contract.overdueSince());
        }
        BigDecimal before = deposit(base.content());
        BigDecimal after = deposit(built.content());
        if (before.compareTo(after) != 0) {
            events.publishEvent(new DepositAmountChanged(contract.id(), after));
        }
        addenda.markApplied(addendum.id(), versionId);
        addenda.setPending(request.id(), false);
        auditLog.write(AuditLogEntry.builder()
                .action(ADDENDUM_APPLIED)
                .entity(ADDENDUM_ENTITY, addendum.id())
                .resident(contract.residentId())
                .before(Map.of("status", Addendum.Status.PENDING_EFFECTIVE.name()))
                .after(Map.of("status", Addendum.Status.APPLIED.name(), "contentVersionId", versionId))
                .basis(new Basis(null, request.id(), contract.id(), null))
                .build());
        events.publishEvent(new AddendumApplied(
                addendum.id(), contract.id(), contract.residentId(), names(change.changeTypes()), effective));
        return new ResultRef(ADDENDUM_ENTITY, String.valueOf(addendum.id()));
    }

    /** DUOC_THAY_THE khi lần đánh giá được xác nhận (FR-036a của 001), chỉ hệ thống chuyển. */
    @Override
    public ExtendedTransitions extendedTransitions() {
        return new ExtendedTransitions(Map.of(
                SUPERSEDE,
                new ExtendedTransitions.Transition(
                        Set.of(ApprovalStatus.DRAFT, ApprovalStatus.PENDING_APPROVAL, ApprovalStatus.APPROVED),
                        SUPERSEDED)));
    }

    // ---- Chi tiết -------------------------------------------------------------------------------------------------

    Optional<ContractVersion> base(long contractId) {
        return contracts.latestVersion(contractId);
    }

    /** Ngày hiệu lực thực tế nếu duyệt hôm nay: ngày mong muốn, hoặc hôm nay khi ngày mong muốn đã qua (FR-037). */
    LocalDate effectiveOn(ApprovalRequestView request) {
        LocalDate today = time.today();
        return request.requestedEffectiveDate().isAfter(today) ? request.requestedEffectiveDate() : today;
    }

    static BigDecimal deposit(JsonNode content) {
        if (content.path("noDepositRequired").asBoolean(false) || !content.hasNonNull("depositAmount")) {
            return BigDecimal.ZERO;
        }
        return content.get("depositAmount").decimalValue();
    }

    static Set<String> names(Set<ChangeType> types) {
        return types.stream().map(ChangeType::name).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    static Set<ChangeType> typesOf(Set<String> names) {
        Set<ChangeType> types = EnumSet.noneOf(ChangeType.class);
        names.forEach(name -> types.add(ChangeType.valueOf(name)));
        return types;
    }

    /** Tra cứu chỉ đọc cho bản diff lúc duyệt: không đánh dấu phiên bản đơn giá đã tham chiếu. */
    private class DiffPricing implements AddendumContent.Pricing {

        private final LocalDate date;

        DiffPricing(LocalDate date) {
            this.date = date;
        }

        @Override
        public Optional<Long> careServiceOf(CareLevel level) {
            return prices.careServiceOf(level).map(ServiceView::id);
        }

        @Override
        public Optional<BigDecimal> unitPrice(long serviceId) {
            return prices.unitPriceAt(serviceId, date, false).map(UnitPriceView::unitPrice);
        }

        @Override
        public boolean isCareService(long serviceId) {
            return contracts
                    .serviceKind(serviceId)
                    .map(k -> "CARE".equals(k.type()))
                    .orElse(false);
        }
    }

    /** Tra cứu lúc áp dụng: đơn giá ghi vào phiên bản đánh dấu phiên bản giá đã tham chiếu (BR-M11-02, FR-036). */
    private class AppliedPricing extends DiffPricing {

        private final LocalDate date;

        AppliedPricing(LocalDate date) {
            super(date);
            this.date = date;
        }

        @Override
        public Optional<BigDecimal> unitPrice(long serviceId) {
            return prices.unitPriceAt(serviceId, date, true).map(UnitPriceView::unitPrice);
        }
    }
}
