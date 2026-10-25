package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.api.UnitPriceApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.UnitPriceView;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import com.cambae.backend.m02.api.spi.RepresentativePort;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ArrivalLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ServiceLine;
import com.cambae.backend.m02.internal.contract.domain.NonStandardTerm;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore.ServiceKind;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.CreateRequest;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hợp đồng lưu trú (004 US3; FR-019 → FR-029, FR-036, DBR-02, DBR-06): lập, sửa Nháp, gửi duyệt điều khoản khác chuẩn, Gửi
 * ký, Trả về nháp, Ghi nhận đã ký, Hủy. Hành chính thực hiện mọi lệnh (FR-028, Phụ lục 27 chú thích 26); Kế toán có ô T ở
 * dòng này chỉ để thu cọc ở 017 nên không được lệnh nào ở đây. Mọi lệnh khóa {@code resident_residency} trước, rồi dòng
 * hợp đồng (research T-03). Kiểm giường (nội trú) và sức chứa khu nghỉ (bán trú) khi Ghi nhận đã ký chờ 003 công bố giao
 * diện nên chưa có (FR-021, research T-10).
 */
@Service
public class ContractService {

    public static final String ENTITY_TYPE = "M02.CONTRACT";
    public static final String SCAN_OWNER_TYPE = "M02.CONTRACT_SCAN";

    static final AuditAction CREATED = new AuditAction("M02_CONTRACT_CREATED", false);
    static final AuditAction UPDATED = new AuditAction("M02_CONTRACT_UPDATED", false);
    static final AuditAction TERMS_SUBMITTED = new AuditAction("M02_CONTRACT_TERMS_SUBMITTED", true);
    static final AuditAction SENT = new AuditAction("M02_CONTRACT_SENT", false);
    static final AuditAction RETURNED = new AuditAction("M02_CONTRACT_RETURNED", true);
    static final AuditAction SIGNED = new AuditAction("M02_CONTRACT_SIGNED", false);
    static final AuditAction CANCELLED = new AuditAction("M02_CONTRACT_CANCELLED", true);

    /** Thân hợp đồng kèm cảnh báo FR-025 (không chặn). */
    public record Result(Contract contract, List<Warning> warnings) {}

    private static final Set<ResidentStatus> ELIGIBLE = Set.of(
            ResidentStatus.ADMISSION_IN_PROGRESS,
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED);

    private final ContractStore store;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final CareLevelApi careLevels;
    private final UnitPriceApi prices;
    private final ParameterApi parameters;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<RepresentativePort> representatives;
    private final ObjectProvider<ApprovalRequestApi> approvals;

    ContractService(
            ContractStore store,
            ResidencyLock residencyLock,
            ResidentApi residents,
            CareLevelApi careLevels,
            UnitPriceApi prices,
            ParameterApi parameters,
            FileApi files,
            AuditLogApi auditLog,
            BusinessTime time,
            ApplicationEventPublisher events,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<RepresentativePort> representatives,
            ObjectProvider<ApprovalRequestApi> approvals) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.careLevels = careLevels;
        this.prices = prices;
        this.parameters = parameters;
        this.files = files;
        this.auditLog = auditLog;
        this.time = time;
        this.events = events;
        this.currentUser = currentUser;
        this.representatives = representatives;
        this.approvals = approvals;
    }

    public static Target target(long contractId, long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(contractId), residentId, null);
    }

    /** Đối tượng kiểm quyền của lệnh lập: người cao tuổi, chưa có hợp đồng. */
    public static Target targetForResident(long residentId) {
        return new Target(ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một hợp đồng; không có thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long contractId) {
        Contract contract = store.find(contractId).orElseThrow(NotFoundException::new);
        return target(contract.id(), contract.residentId());
    }

    // ---- Đọc ------------------------------------------------------------------------------------------------------

    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Contract get(Target target, long contractId) {
        return store.find(contractId).orElseThrow(NotFoundException::new);
    }

    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Contract> list(Target target, Long residentId, Set<ContractStatus> statuses, Boolean overdue) {
        return store.list(residentId, statuses, overdue);
    }

    /** Điều khoản khác chuẩn của hợp đồng Nháp (FR-025a); hợp đồng khác Nháp không còn điều khoản cần duyệt. */
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<NonStandardTerm> nonStandardTerms(Target target, long contractId) {
        Contract contract = store.find(contractId).orElseThrow(NotFoundException::new);
        return nonStandardTerms(contract);
    }

    List<NonStandardTerm> nonStandardTerms(Contract contract) {
        ContractContent content = contract.content();
        return NonStandardTermDetector.detect(
                content,
                serviceId -> prices.unitPriceAt(serviceId, content.startDate(), false)
                        .map(UnitPriceView::unitPrice),
                parameters.read(ParameterCode.CFG_M02_05).raw());
    }

    // ---- Lệnh -----------------------------------------------------------------------------------------------------

    /** Lập hợp đồng Nháp (FR-019): người cao tuổi Đang tiếp nhận hoặc đang lưu trú, đã có người đại diện (012). */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.CREATE)
    @Transactional
    public Result create(Target target, ContractContent content) {
        requireOffice();
        long residentId = target.residentId();
        residencyLock.lock(residentId);
        if (!ELIGIBLE.contains(residents.status(residentId))) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENT_NOT_ELIGIBLE_FOR_CONTRACT",
                    "Chỉ lập hợp đồng cho người cao tuổi Đang tiếp nhận hoặc đang lưu trú (FR-019)");
        }
        RepresentativePort port = representatives.getIfAvailable();
        if (port == null || !port.hasRepresentative(residentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "REPRESENTATIVE_REQUIRED",
                    "Người cao tuổi chưa có người đại diện hiệu lực để lập hợp đồng (FR-019, 012)");
        }
        ContractContent normalized = normalize(content);
        validate(normalized);
        long id = store.insert(
                residentId,
                normalized,
                time.today().getYear(),
                currentUser.getObject().accountId(),
                time.now());
        Contract created = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(created))
                .build());
        return new Result(created, warnings(created));
    }

    /**
     * Sửa nội dung Nháp (FR-020): chỉ khi Nháp và không chờ duyệt điều khoản (409 {@code CONTRACT_LOCKED}); {@code
     * If-Match} sai thì 412. Nội dung đổi làm bản duyệt điều khoản mất hiệu lực (FR-025a).
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public Result update(Target target, long contractId, String ifMatch, long residentId, ContractContent content) {
        requireOffice();
        Contract current = lockedContract(contractId);
        if (current.status() != ContractStatus.DRAFT || current.awaitingTermsApproval()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONTRACT_LOCKED",
                    "Nội dung hợp đồng chỉ sửa được khi Nháp và không chờ duyệt điều khoản (FR-020, FR-023)");
        }
        VersionEtag.requireMatch(ifMatch, current.version(), current);
        if (residentId != current.residentId()) {
            throw new InvalidValueException("residentId của hợp đồng không đổi được");
        }
        ContractContent normalized = normalize(content);
        validate(normalized);
        Long approvalId = current.termsApprovalRequestId();
        String approvedHash = current.contentHash();
        if (approvalId != null && !ContractSnapshot.hash(residentId, normalized).equals(approvedHash)) {
            approvalId = null;
            approvedHash = null;
        }
        if (store.updateContent(contractId, current.version(), normalized, approvalId, approvedHash) == 0) {
            throw new VersionMismatchException(current);
        }
        Contract updated = store.find(contractId).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(UPDATED)
                .entity(ENTITY_TYPE, contractId)
                .resident(residentId)
                .before(snapshot(current))
                .after(snapshot(updated))
                .build());
        return new Result(updated, warnings(updated));
    }

    /**
     * Gửi Quản lý viện duyệt điều khoản khác chuẩn (FR-025a): lập yêu cầu {@code M02.CONTRACT_TERMS_APPROVAL} ở Chờ duyệt
     * và khóa nội dung tới khi yêu cầu có quyết định.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public ApprovalRequestView submitTermsForApproval(Target target, long contractId, Reason reason) {
        requireOffice();
        Contract contract = lockedContract(contractId);
        requireStatus(contract, ContractStatus.DRAFT);
        if (contract.awaitingTermsApproval()) {
            throw locked();
        }
        List<NonStandardTerm> terms = nonStandardTerms(contract);
        if (terms.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_NON_STANDARD_TERMS",
                    "Hợp đồng không có điều khoản khác chuẩn nên không cần duyệt (FR-025a)");
        }
        String hash = ContractSnapshot.hash(contract.residentId(), contract.content());
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("contractId", contractId);
        content.put("contentHash", hash);
        ArrayNode list = content.putArray("terms");
        for (NonStandardTerm term : terms) {
            ObjectNode item = list.addObject();
            item.put("type", term.type().name());
            item.put("description", term.description());
            item.put("standardValue", term.standardValue());
            item.put("proposedValue", term.proposedValue());
        }
        ApprovalRequestView request = approvals
                .getObject()
                .create(new CreateRequest(ContractTermsRequestHandler.TYPE, content, time.today(), reason, true));
        store.awaitTerms(contractId, request.id(), hash);
        auditLog.write(AuditLogEntry.builder()
                .action(TERMS_SUBMITTED)
                .entity(ENTITY_TYPE, contractId)
                .resident(contract.residentId())
                .details(Map.of("approvalRequestId", request.id()))
                .reason(reason)
                .build());
        return request;
    }

    /** Gửi ký (FR-020): khóa nội dung; chặn khi thiếu nội dung bắt buộc, dịch vụ chăm sóc lệch mức, điều khoản chưa duyệt. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public Result sendForSignature(Target target, long contractId) {
        requireOffice();
        Contract contract = lockedContract(contractId);
        requireStatus(contract, ContractStatus.DRAFT);
        List<UnmetCondition> unmet = sendConditions(contract);
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        transition(contract, ContractStatus.PENDING_SIGNATURE, SENT, null);
        Contract sent = store.find(contractId).orElseThrow();
        return new Result(sent, warnings(sent));
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public Result returnToDraft(Target target, long contractId, Reason reason) {
        requireOffice();
        Contract contract = lockedContract(contractId);
        requireStatus(contract, ContractStatus.PENDING_SIGNATURE);
        transition(contract, ContractStatus.DRAFT, RETURNED, reason);
        return new Result(store.find(contractId).orElseThrow(), List.of());
    }

    /**
     * Ghi nhận đã ký (FR-021, FR-022, FR-036): ngày ký, người đại diện hiệu lực (DBR-02), bản scan; không chồng thời gian
     * với hợp đồng Hiệu lực khác; ghi phiên bản nội dung 0 với đơn giá của mọi khoản và đánh dấu phiên bản giá đã tham chiếu.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public Result recordSigned(
            Target target, long contractId, LocalDate signedDate, long signingRepresentativeId, long scanFileId) {
        requireOffice();
        Contract contract = lockedContract(contractId);
        requireStatus(contract, ContractStatus.PENDING_SIGNATURE);
        long residentId = contract.residentId();
        RepresentativePort port = representatives.getIfAvailable();
        if (port == null || !port.isActiveRepresentative(signingRepresentativeId, residentId, signedDate)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_SIGNING_REPRESENTATIVE",
                    "Người ký không phải người đại diện hiệu lực của người cao tuổi (DBR-02)");
        }
        ContractContent content = contract.content();
        if (store.overlapsActive(residentId, contractId, content.startDate(), content.endDate())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONTRACT_OVERLAP",
                    "Người cao tuổi đã có hợp đồng Hiệu lực chồng khoảng thời gian (FR-022, DBR-06)");
        }
        Map<Long, BigDecimal> recorded = recordPrices(contract);
        files.attach(scanFileId, SCAN_OWNER_TYPE, contractId);
        store.markSigned(contractId, signedDate, signingRepresentativeId, scanFileId, content.endDate());
        recorded.forEach((serviceId, price) -> store.setRecordedUnitPrice(contractId, serviceId, price));
        store.insertVersion(
                contractId, 0, content.startDate(), ContractSnapshot.of(residentId, content, recorded), content);
        Contract signed = store.find(contractId).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(SIGNED)
                .entity(ENTITY_TYPE, contractId)
                .resident(residentId)
                .before(Map.of("status", contract.status().name()))
                .after(signedSnapshot(signed, recorded))
                .build());
        events.publishEvent(new ContractStatusChanged(
                contractId, residentId, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE, null));
        return new Result(signed, List.of());
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public Result cancel(Target target, long contractId, Reason reason) {
        requireOffice();
        Contract contract = lockedContract(contractId);
        if (contract.status() != ContractStatus.DRAFT && contract.status() != ContractStatus.PENDING_SIGNATURE) {
            throw invalidTransition(contract);
        }
        transition(contract, ContractStatus.CANCELLED, CANCELLED, reason);
        return new Result(store.find(contractId).orElseThrow(), List.of());
    }

    // ---- Chi tiết -------------------------------------------------------------------------------------------------

    /** Khóa người rồi hợp đồng (research T-03); không có hợp đồng thì 404. */
    private Contract lockedContract(long contractId) {
        Contract found = store.find(contractId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        return store.lock(contractId).orElseThrow(NotFoundException::new);
    }

    private void transition(Contract contract, ContractStatus to, AuditAction action, Reason reason) {
        if (store.transition(contract.id(), contract.status(), to) == 0) {
            throw invalidTransition(contract);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY_TYPE, contract.id())
                .resident(contract.residentId())
                .before(Map.of("status", contract.status().name()))
                .after(Map.of("status", to.name()))
                .reason(reason)
                .build());
        events.publishEvent(new ContractStatusChanged(
                contract.id(), contract.residentId(), contract.status(), to, reason == null ? null : reason.value()));
    }

    private void requireStatus(Contract contract, ContractStatus expected) {
        if (contract.status() != expected) {
            throw invalidTransition(contract);
        }
    }

    private static BusinessException invalidTransition(Contract contract) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "INVALID_TRANSITION",
                "Lệnh không áp dụng được cho hợp đồng ở trạng thái " + contract.status() + " (FR-020)");
    }

    private static BusinessException locked() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "CONTRACT_LOCKED",
                "Hợp đồng đang chờ duyệt điều khoản khác chuẩn nên nội dung bị khóa (FR-025a)");
    }

    /** Chỉ Hành chính thực hiện lệnh hợp đồng; Kế toán có ô T ở dòng này để thu cọc ở 017 (Phụ lục 27 chú thích 26). */
    private void requireOffice() {
        if (!currentUser.getObject().roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính thực hiện lệnh hợp đồng (FR-028)");
        }
    }

    private void validate(ContractContent content) {
        ContractRules.validate(content);
        for (ServiceLine line : content.services()) {
            if (!store.serviceExists(line.serviceId())) {
                throw new InvalidValueException("Dịch vụ " + line.serviceId() + " không tồn tại");
            }
        }
    }

    /** Số lượng mặc định 1. */
    private static ContractContent normalize(ContractContent content) {
        List<ServiceLine> services = content.services().stream()
                .map(line -> line.quantity() == null
                        ? new ServiceLine(line.serviceId(), line.inPackage(), BigDecimal.ONE, line.customUnitPrice())
                        : line)
                .toList();
        return content.withLines(services, content.absencePolicyOverrides(), content.arrivalSchedule());
    }

    /** Điều kiện chặn Gửi ký (FR-019, FR-025, FR-025a). */
    private List<UnmetCondition> sendConditions(Contract contract) {
        ContractContent content = contract.content();
        List<UnmetCondition> unmet = new ArrayList<>();
        boolean inpatient = content.residenceType() != ResidenceType.DAY_CARE;
        if (inpatient && content.roomTypeId() == null && content.bedId() == null) {
            unmet.add(condition("CONTRACT_ROOM_REQUIRED", "Hợp đồng nội trú cần loại phòng hoặc giường", "FR-019"));
        }
        if (!inpatient && content.arrivalSchedule().isEmpty()) {
            unmet.add(condition(
                    "CONTRACT_ARRIVAL_SCHEDULE_REQUIRED", "Hợp đồng bán trú cần lịch đến theo buổi", "FR-019"));
        }
        if (!content.noDepositRequired() && content.depositAmount() == null) {
            unmet.add(condition(
                    "CONTRACT_DEPOSIT_REQUIRED",
                    "Cần ghi khoản đặt cọc hoặc chọn không yêu cầu đặt cọc",
                    "FR-019, FR-030"));
        }
        if (!hasExactlyOneMatchingCareService(content)) {
            unmet.add(condition(
                    "CONTRACT_CARE_SERVICE_REQUIRED",
                    "Hợp đồng cần đúng một dịch vụ chăm sóc, đúng mức " + content.careLevel() + " của hợp đồng",
                    "FR-025, Q-297"));
        }
        if (!nonStandardTerms(contract).isEmpty() && !termsApproved(contract)) {
            unmet.add(condition(
                    "TERMS_APPROVAL_REQUIRED",
                    "Điều khoản khác chuẩn chưa được Quản lý viện duyệt cho đúng nội dung hiện tại",
                    "FR-025a"));
        }
        return unmet;
    }

    private static UnmetCondition condition(String code, String description, String basis) {
        return new UnmetCondition(code, description, basis, "004", false);
    }

    private boolean hasExactlyOneMatchingCareService(ContractContent content) {
        List<ServiceKind> care = content.services().stream()
                .map(line -> store.serviceKind(line.serviceId()).orElseThrow())
                .filter(kind -> "CARE".equals(kind.type()))
                .toList();
        return care.size() == 1 && care.get(0).careLevel() == content.careLevel();
    }

    private boolean termsApproved(Contract contract) {
        return contract.termsApprovalRequestId() != null
                && !contract.awaitingTermsApproval()
                && ContractSnapshot.hash(contract.residentId(), contract.content())
                        .equals(contract.contentHash());
    }

    /**
     * Đơn giá ghi lúc ký (FR-036): giá riêng nếu có, không thì đơn giá chuẩn tại ngày bắt đầu, và đánh dấu phiên bản giá đã
     * tham chiếu. Dịch vụ không có giá hiệu lực ngày đó thì chặn.
     */
    private Map<Long, BigDecimal> recordPrices(Contract contract) {
        LocalDate start = contract.content().startDate();
        Map<Long, BigDecimal> recorded = new LinkedHashMap<>();
        List<UnmetCondition> missing = new ArrayList<>();
        for (ServiceLine line : contract.content().services()) {
            if (line.customUnitPrice() != null) {
                recorded.put(line.serviceId(), line.customUnitPrice());
                continue;
            }
            Optional<UnitPriceView> price = prices.unitPriceAt(line.serviceId(), start, true);
            if (price.isEmpty()) {
                missing.add(condition(
                        "SERVICE_PRICE_MISSING",
                        "Dịch vụ " + line.serviceId() + " không có đơn giá hiệu lực tại ngày bắt đầu " + start,
                        "FR-036"));
            } else {
                recorded.put(line.serviceId(), price.get().unitPrice());
            }
        }
        if (!missing.isEmpty()) {
            throw new ConditionNotMetException(missing);
        }
        return recorded;
    }

    /** Cảnh báo không chặn (FR-025): mức khác mức hiện hành; dịch vụ hoặc đơn giá không hiệu lực tại ngày bắt đầu. */
    List<Warning> warnings(Contract contract) {
        ContractContent content = contract.content();
        List<Warning> warnings = new ArrayList<>();
        Optional<CareLevel> current = careLevels.current(contract.residentId());
        if (current.isPresent() && current.get() != content.careLevel()) {
            warnings.add(new Warning(
                    "CONTRACT_CARE_LEVEL_MISMATCH",
                    "Mức chăm sóc hợp đồng (" + content.careLevel() + ") khác mức hiện hành (" + current.get() + ")"));
        }
        for (ServiceLine line : content.services()) {
            boolean active =
                    store.serviceKind(line.serviceId()).map(ServiceKind::active).orElse(false);
            boolean priced = line.customUnitPrice() != null
                    || prices.unitPriceAt(line.serviceId(), content.startDate(), false)
                            .isPresent();
            if (!active || !priced) {
                warnings.add(new Warning(
                        "SERVICE_PRICE_NOT_EFFECTIVE",
                        "Dịch vụ " + line.serviceId() + " hoặc đơn giá không hiệu lực tại ngày bắt đầu "
                                + content.startDate()));
            }
        }
        return warnings;
    }

    // ---- Chụp cho nhật ký -----------------------------------------------------------------------------------------

    static Map<String, Object> snapshot(Contract contract) {
        ContractContent content = contract.content();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("residentId", contract.residentId());
        map.put("status", contract.status().name());
        map.put("residenceType", content.residenceType().name());
        map.put("startDate", content.startDate().toString());
        map.put("endDate", content.endDate() == null ? null : content.endDate().toString());
        map.put("careLevel", content.careLevel().name());
        map.put("roomTypeId", content.roomTypeId());
        map.put("bedId", content.bedId());
        map.put("residencyFeeBillingPeriod", content.residencyFeeBillingPeriod().name());
        map.put(
                "services",
                content.services().stream()
                        .map(line -> line.serviceId() + ":"
                                + line.quantity().stripTrailingZeros().toPlainString() + ":" + line.customUnitPrice())
                        .toList());
        map.put("noDepositRequired", content.noDepositRequired());
        map.put("depositAmount", content.depositAmount());
        map.put("bedHoldPolicy", content.bedHoldPolicy());
        map.put(
                "absencePolicyOverrides",
                content.absencePolicyOverrides().stream()
                        .map(ContractService::describe)
                        .toList());
        map.put(
                "arrivalSchedule",
                content.arrivalSchedule().stream()
                        .map(ContractService::describe)
                        .toList());
        map.put("terminationCondition", content.terminationCondition());
        return map;
    }

    private static Map<String, Object> signedSnapshot(Contract contract, Map<Long, BigDecimal> recorded) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", contract.status().name());
        map.put("signedDate", contract.signedDate().toString());
        map.put("signingRepresentativeId", contract.signingRepresentativeId());
        map.put("scanFileId", contract.scanFileId());
        map.put("recordedUnitPrices", recorded.toString());
        return map;
    }

    private static String describe(AbsencePolicyLine line) {
        return line.residenceType() + "/" + line.absenceType() + "/" + line.fromDay() + "-" + line.toDay() + "/"
                + line.coefficient() + "/" + line.holdBed();
    }

    private static String describe(ArrivalLine line) {
        return (line.dayOfWeek() == null ? line.date() : "thứ " + line.dayOfWeek()) + " " + line.arrivalTime() + "-"
                + line.departureTime();
    }
}
