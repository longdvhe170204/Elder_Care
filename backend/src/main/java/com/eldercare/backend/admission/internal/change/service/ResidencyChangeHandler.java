package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.admission.api.events.AddendumAppliedNotice;
import com.eldercare.backend.admission.api.events.DepositAmountChanged;
import com.eldercare.backend.admission.api.events.ResidenceTypeChanged;
import com.eldercare.backend.admission.api.events.ResidencyChangeOutcomeNotice;
import com.eldercare.backend.admission.api.events.ResidencyChangeReturnedToDraft;
import com.eldercare.backend.admission.api.events.ResidencyChangeSubmittedNotice;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.change.domain.ChangeType;
import com.eldercare.backend.admission.internal.change.domain.RegistrableServiceTypes;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent.ServiceChange;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeDigest;
import com.eldercare.backend.admission.internal.change.domain.ResidentOpinion;
import com.eldercare.backend.admission.internal.change.persistence.AddendumStore;
import com.eldercare.backend.admission.internal.change.persistence.ResidentOpinionStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractVersion;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.contract.service.ContractService;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.AssessmentApi;
import com.eldercare.backend.resident.api.CareLevelApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.AssessmentSummary;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.RoomTypeRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
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
    static final String CONFIRMATION_REQUIRED = "REPRESENTATIVE_CONFIRMATION_REQUIRED";
    static final String RESIDENCE_TYPE_UNCHANGED = "RESIDENCE_TYPE_UNCHANGED";
    static final String ARRIVAL_SCHEDULE_UNCHANGED = "ARRIVAL_SCHEDULE_UNCHANGED";
    static final String ARRIVAL_SCHEDULE_DUPLICATE_DAY = "ARRIVAL_SCHEDULE_DUPLICATE_DAY";
    static final String SAME_ROOM_TYPE = "BED_CHANGE_SAME_ROOM_TYPE";
    static final String CARE_LEVEL_NOT_FROM_ASSESSMENT = "CARE_LEVEL_NOT_FROM_ASSESSMENT";
    static final String RESIDENT_DISAGREE_REASON_REQUIRED = "RESIDENT_DISAGREE_REASON_REQUIRED";
    static final String RESIDENT_OPINION_MISSING = "RESIDENT_OPINION_MISSING";
    static final String RESIDENT_OPINION_OUTDATED = "RESIDENT_OPINION_OUTDATED";
    static final AuditAction ADDENDUM_CREATED = new AuditAction("M02_ADDENDUM_CREATED", false);
    static final AuditAction ADDENDUM_APPLIED = new AuditAction("M02_ADDENDUM_APPLIED", false);
    static final AuditAction ADDENDUM_NOT_APPLIED = new AuditAction("M02_ADDENDUM_NOT_APPLIED", false);
    static final AuditAction ADDENDUM_CANCELLED = new AuditAction("M02_ADDENDUM_CANCELLED", false);
    static final AuditAction ADDENDUM_SIGNED = new AuditAction("M02_ADDENDUM_SIGNED", false);

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
    private final ObjectProvider<AccountApi> accounts;

    private final ObjectProvider<BedAllocationCommandApi> beds;
    private final ObjectProvider<LocationStructureApi> structure;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final ChangeConfirmations confirmations;
    private final ObjectProvider<AssessmentApi> assessments;
    private final ResidentOpinionStore opinions;
    private final ObjectProvider<ResidentApi> residents;

    ResidencyChangeHandler(
            ChangeConfirmations confirmations,
            ObjectProvider<AssessmentApi> assessments,
            ResidentOpinionStore opinions,
            ObjectProvider<BedAllocationCommandApi> beds,
            ObjectProvider<LocationStructureApi> structure,
            ObjectProvider<ApprovalRequestApi> approvals,
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
            ObjectProvider<RepresentativePort> representatives,
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<ResidentApi> residents) {
        this.confirmations = confirmations;
        this.assessments = assessments;
        this.opinions = opinions;
        this.beds = beds;
        this.structure = structure;
        this.approvals = approvals;
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
        this.accounts = accounts;
        this.residents = residents;
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
            throw new PermissionDeniedException("Bác sĩ chỉ có quyền lập yêu cầu thay đổi mức chăm sóc.");
        }
        if (user.type() == AccountType.FAMILY_MEMBER) {
            Contract contract = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
            RepresentativePort port = representatives.getIfAvailable();
            boolean representative = port != null
                    && user.familyMemberId()
                            .map(id -> port.isActiveRepresentative(id, contract.residentId(), time.today()))
                            .orElse(false);
            if (!representative) {
                throw new PermissionDeniedException(
                        "Chỉ người đại diện hợp pháp đang có hiệu lực mới được lập yêu cầu qua cổng thông tin gia đình.");
            }
        }
        List<UnmetCondition> serviceTypes = serviceTypeConditions(change);
        if (!serviceTypes.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    RegistrableServiceTypes.CODE,
                    serviceTypes.stream().map(UnmetCondition::description).collect(Collectors.joining("; ")));
        }
        List<UnmetCondition> careLevel = careLevelConditions(change);
        if (!careLevel.isEmpty()) {
            throw new ConditionNotMetException(careLevel);
        }
        List<UnmetCondition> roomType = roomTypeConditions(change);
        if (!roomType.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    SAME_ROOM_TYPE,
                    roomType.stream().map(UnmetCondition::description).collect(Collectors.joining("; ")));
        }
        List<UnmetCondition> residenceType = residenceTypeConditions(change);
        if (!residenceType.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    RESIDENCE_TYPE_UNCHANGED,
                    residenceType.stream().map(UnmetCondition::description).collect(Collectors.joining("; ")));
        }
        List<UnmetCondition> arrivalSchedule = arrivalScheduleConditions(change);
        if (!arrivalSchedule.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    arrivalSchedule.getFirst().code(),
                    arrivalSchedule.stream().map(UnmetCondition::description).collect(Collectors.joining("; ")));
        }
    }

    /**
     * Lịch đến mới không được trùng thứ/ngày giữa các dòng và không được trùng hoàn toàn lịch đến hiện hành của hợp
     * đồng bán trú (khi không đồng thời đổi loại lưu trú). Kiểm khi lập, sửa (422 qua {@link #validateContent}), gửi
     * duyệt và duyệt (409 cùng mã).
     */
    List<UnmetCondition> arrivalScheduleConditions(ResidencyChangeContent change) {
        if (!change.has(ChangeType.ARRIVAL_SCHEDULE) || change.arrivalSchedule().isEmpty()) {
            return List.of();
        }
        Set<String> seenKeys = new java.util.HashSet<>();
        for (ResidencyChangeContent.ArrivalChange line : change.arrivalSchedule()) {
            String key = line.dayOfWeek() != null ? "W:" + line.dayOfWeek() : "D:" + line.date();
            if (!seenKeys.add(key)) {
                return List.of(new UnmetCondition(
                        ARRIVAL_SCHEDULE_DUPLICATE_DAY, "Mỗi thứ hoặc ngày trong lịch đến chỉ được chọn một dòng."));
            }
        }
        if (change.has(ChangeType.RESIDENCE_TYPE)) {
            return List.of();
        }
        Optional<ContractVersion> base = base(change.contractId());
        if (base.isEmpty() || baseType(base.get()) != ResidenceType.DAY_CARE) {
            return List.of();
        }
        Set<ResidencyChangeContent.ArrivalChange> current = new java.util.HashSet<>();
        for (JsonNode node : base.get().content().path("arrivalSchedule")) {
            Integer day = node.hasNonNull("dayOfWeek") ? node.get("dayOfWeek").asInt() : null;
            LocalDate date =
                    node.hasNonNull("date") ? LocalDate.parse(node.get("date").asString()) : null;
            java.time.LocalTime arrival =
                    java.time.LocalTime.parse(node.path("arrivalTime").asString(""));
            java.time.LocalTime departure =
                    java.time.LocalTime.parse(node.path("departureTime").asString(""));
            current.add(new ResidencyChangeContent.ArrivalChange(day, date, arrival, departure));
        }
        if (!current.isEmpty() && current.equals(new java.util.HashSet<>(change.arrivalSchedule()))) {
            return List.of(new UnmetCondition(
                    ARRIVAL_SCHEDULE_UNCHANGED, "Lịch đến mới trùng với lịch đến hiện hành của hợp đồng."));
        }
        return List.of();
    }

    /**
     * Loại lưu trú mới không được trùng loại lưu trú hiện hành của hợp đồng. Kiểm khi lập, sửa (422 qua {@link
     * #validateContent}), gửi duyệt (409 cùng mã qua {@link #checkOnSubmit}) và duyệt.
     */
    List<UnmetCondition> residenceTypeConditions(ResidencyChangeContent change) {
        if (!change.has(ChangeType.RESIDENCE_TYPE) || change.residenceType() == null) {
            return List.of();
        }
        Optional<ContractVersion> base = base(change.contractId());
        if (base.isEmpty()) {
            return List.of();
        }
        if (baseType(base.get()) == change.residenceType()) {
            return List.of(new UnmetCondition(
                    RESIDENCE_TYPE_UNCHANGED, "Loại lưu trú mới trùng loại lưu trú hiện hành của hợp đồng."));
        }
        return List.of();
    }

    /**
     * FR-043l (Q-445, R22): loại thay đổi "Loại phòng" (mã {@code BED}) chỉ dùng khi giường mới thuộc loại phòng khác loại
     * phòng của hợp đồng hiện hành; cùng loại phòng thì không đổi điều khoản, dùng Chuyển giường trực tiếp (7.4). Không xét
     * khi đổi giường đi kèm bán trú sang nội trú (giường đầu tiên), khi yêu cầu lập sau chuyển giường gấp, hoặc khi hợp đồng
     * chưa ghi loại phòng. Lập, sửa: 422 qua {@link #validateContent}; gửi duyệt, duyệt: 409 cùng các điều kiện khác.
     */
    List<UnmetCondition> roomTypeConditions(ResidencyChangeContent change) {
        if (!change.has(ChangeType.BED) || change.newBedId() == null || change.afterUrgentTransfer()) {
            return List.of();
        }
        Optional<ContractVersion> base = base(change.contractId());
        if (base.isEmpty() || change.firstBed(baseType(base.get()))) {
            return List.of();
        }
        JsonNode current = base.get().content().get("roomTypeId");
        if (current == null || current.isNull()) {
            return List.of();
        }
        boolean same = structure
                .getObject()
                .roomTypeOfBed(change.newBedId())
                .map(RoomTypeRef::id)
                .filter(id -> id == current.asLong())
                .isPresent();
        return same
                ? List.of(new UnmetCondition(
                        SAME_ROOM_TYPE,
                        "Giường mới cùng loại phòng với hợp đồng hiện hành: dùng Chuyển giường trực tiếp, không cần lập"
                                + " yêu cầu điều chỉnh hợp đồng và không ký phụ lục"))
                : List.of();
    }

    /**
     * FR-043k (Q-426, R21): dòng thêm dịch vụ chỉ thuộc phục hồi chức năng, hoạt động, đưa đi khám, tiêm; bớt không xét
     * loại. Lập, sửa: 422 qua {@link #validateContent}; gửi duyệt, duyệt: 409 cùng các điều kiện khác (yêu cầu lập trước khi
     * có quy tắc).
     */
    List<UnmetCondition> serviceTypeConditions(ResidencyChangeContent change) {
        if (!change.has(ChangeType.SERVICE)) {
            return List.of();
        }
        List<UnmetCondition> unmet = new ArrayList<>();
        for (ServiceChange line : change.services()) {
            if (line.action() != ServiceChange.Action.ADD) {
                continue;
            }
            contracts
                    .serviceKind(line.serviceId())
                    .filter(kind -> !RegistrableServiceTypes.registrable(kind.type()))
                    .ifPresent(kind -> unmet.add(new UnmetCondition(
                            RegistrableServiceTypes.CODE,
                            RegistrableServiceTypes.description(line.serviceId(), kind.type()))));
        }
        return unmet;
    }

    /**
     * FR-043j (R20): mức chăm sóc mới phải bằng mức của lần đánh giá gần nhất đã xác nhận, và mức đó khác mức của hợp đồng
     * hiện hành. Kiểm khi lập, sửa (409 qua {@link #validateContent}), gửi duyệt và duyệt (cùng các điều kiện khác).
     */
    List<UnmetCondition> careLevelConditions(ResidencyChangeContent change) {
        if (!change.has(ChangeType.CARE_LEVEL)) {
            return List.of();
        }
        Contract contract = contracts.find(change.contractId()).orElse(null);
        Optional<ContractVersion> base = contract == null ? Optional.empty() : base(contract.id());
        if (contract == null || base.isEmpty()) {
            return List.of();
        }
        String contractLevel = base.get().content().path("careLevel").asString("");
        Optional<AssessmentSummary> latest = assessments.getObject().latestConfirmed(contract.residentId());
        if (latest.isEmpty()
                || latest.get().careLevel() == null
                || latest.get().careLevel().name().equals(contractLevel)) {
            return List.of(new UnmetCondition(
                    CARE_LEVEL_NOT_FROM_ASSESSMENT,
                    "Chưa có lần đánh giá lại đã xác nhận ra mức chăm sóc khác mức của hợp đồng; Bác sĩ đánh giá lại trước"
                            + " khi lập yêu cầu đổi mức"));
        }
        if (latest.get().careLevel() != change.careLevel()) {
            return List.of(new UnmetCondition(
                    CARE_LEVEL_NOT_FROM_ASSESSMENT,
                    "Mức chăm sóc mới phải là mức của lần đánh giá gần nhất đã xác nhận ("
                            + levelName(latest.get().careLevel()) + "); sửa mức trong yêu cầu cho khớp"));
        }
        return List.of();
    }

    /** Lần đánh giá gần nhất đã xác nhận, để giao diện điền sẵn mức chăm sóc mới (FR-043j). */
    Optional<AssessmentSummary> assessedLevel(long residentId) {
        return assessments.getObject().latestConfirmed(residentId);
    }

    private static String levelName(CareLevel level) {
        return switch (level) {
            case BASIC -> "Chăm sóc cơ bản";
            case REGULAR -> "Chăm sóc thường xuyên";
            case SPECIAL -> "Chăm sóc đặc biệt";
        };
    }

    /**
     * Sửa, gửi duyệt Nháp (FR-039): người yêu cầu, hoặc Hành chính khi người yêu cầu là người đại diện gửi qua cổng người
     * thân (tài khoản người thân) hoặc là Bác sĩ (Hành chính xác nhận với người đại diện, kèm bằng chứng). Hủy vẫn chỉ
     * người yêu cầu (000).
     */
    @Override
    public boolean mayActOnDraft(CurrentUser user, ApprovalRequestView request) {
        if (request.requesterId() == user.accountId()) {
            return true;
        }
        // Hành chính hoàn thiện cả nháp của Bác sĩ (xác nhận với người đại diện, kèm bằng chứng; 2026-11-20)
        return user.roles().contains(Role.OFFICE_STAFF)
                && accounts.getObject()
                        .byId(request.requesterId())
                        .filter(requester -> requester.type() == AccountType.FAMILY_MEMBER
                                || (requester.roles().contains(Role.DOCTOR)
                                        && !requester.roles().contains(Role.OFFICE_STAFF)))
                        .isPresent();
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
        List<UnmetCondition> unmet = new ArrayList<>(
                rules.check(contract, base.get().content(), change, effectiveOn(request), request.id()));
        unmet.addAll(checkOnSubmit(request));
        return unmet;
    }

    /**
     * Yêu cầu do Hành chính hoặc Bác sĩ lập chỉ gửi Quản lý viện duyệt được khi Hành chính đã xác nhận với người đại diện,
     * kèm bằng chứng (chủ dự án yêu cầu 2026-11-20); yêu cầu người đại diện tự gửi qua cổng và thay đổi nhỏ thì không
     * cần. Người đại diện đồng ý trên ứng dụng (FR-043c) thay cho xác nhận kèm bằng chứng. Kiểm lại lúc duyệt.
     */
    @Override
    public List<UnmetCondition> checkOnSubmit(ApprovalRequestView request) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        List<UnmetCondition> content = new ArrayList<>(serviceTypeConditions(change));
        content.addAll(careLevelConditions(change));
        content.addAll(roomTypeConditions(change));
        content.addAll(residenceTypeConditions(change));
        content.addAll(arrivalScheduleConditions(change));
        if (!content.isEmpty()) {
            return content;
        }
        if (!confirmationRequired(request)
                || confirmations
                        .validConfirmation(request.id(), request.content())
                        .isPresent()
                || confirmations.accepted(request.id())) {
            return List.of();
        }
        return List.of(new UnmetCondition(
                CONFIRMATION_REQUIRED,
                "Hành chính cần xác nhận với người đại diện và đính kèm bằng chứng trước khi gửi Quản lý viện duyệt"));
    }

    /**
     * Cần xác nhận khi không phải thay đổi nhỏ (Q-338: chỉ dịch vụ thuộc gói, chi phí không đổi, viện tự quyết; sửa
     * 2026-11-20) và người yêu cầu không phải tài khoản người thân (người đại diện gửi qua cổng đã là xác nhận của chính
     * họ).
     */
    boolean confirmationRequired(ApprovalRequestView request) {
        if (isMinor(request)) {
            return false;
        }
        boolean portal = accounts.getObject()
                .byId(request.requesterId())
                .map(requester -> requester.type() == AccountType.FAMILY_MEMBER)
                .orElse(false);
        if (!portal) {
            return true;
        }
        // FR-043i (Q-418): Hành chính đổi phần thực chất của yêu cầu người đại diện gửi qua cổng thì phải xác nhận lại;
        // yêu cầu gửi trước khi có dấu nội dung thì giữ hành vi cũ
        return opinions.portalDigest(request.id())
                .map(sent ->
                        !sent.equals(ResidencyChangeDigest.of(request.content(), request.requestedEffectiveDate())))
                .orElse(false);
    }

    /**
     * FR-043h (Q-417, R11): như {@link #checkOnSubmit(ApprovalRequestView)}, thêm: ý kiến hiện hành của người cao tuổi là
     * Không đồng ý thì gửi duyệt bắt buộc lý do. Thiếu ý kiến không chặn (chỉ cảnh báo trong phản hồi). Thay đổi nhỏ không xét.
     */
    @Override
    public List<UnmetCondition> checkOnSubmit(
            ApprovalRequestView request, Optional<com.eldercare.backend.common.Reason> reason) {
        List<UnmetCondition> unmet = new ArrayList<>(checkOnSubmit(request));
        if (reason.isEmpty()
                && !isMinor(request)
                && opinions.current(request.id())
                        .map(o -> o.opinion() == ResidentOpinion.Opinion.DISAGREE)
                        .orElse(false)) {
            unmet.add(new UnmetCondition(
                    RESIDENT_DISAGREE_REASON_REQUIRED,
                    "Người cao tuổi không đồng ý với thay đổi này; nhập lý do vẫn gửi duyệt để Quản lý viện xem xét"));
        }
        return unmet;
    }

    /**
     * Cảnh báo không chặn của yêu cầu (FR-043h, R11, R18): chưa ghi ý kiến người cao tuổi, hoặc ý kiến hiện hành ghi trước
     * khi phần thực chất của yêu cầu đổi. Thay đổi nhỏ và yêu cầu đã rời Nháp, Chờ duyệt không cảnh báo.
     */
    List<String> warnings(ApprovalRequestView request) {
        if (!(ApprovalStatus.DRAFT.equals(request.status()) || ApprovalStatus.PENDING_APPROVAL.equals(request.status()))
                || isMinor(request)) {
            return List.of();
        }
        Optional<ResidentOpinion> current = opinions.current(request.id());
        if (current.isEmpty()) {
            return List.of(RESIDENT_OPINION_MISSING);
        }
        return outdated(request, current.get()) ? List.of(RESIDENT_OPINION_OUTDATED) : List.of();
    }

    /** Ý kiến ghi khi phần thực chất của yêu cầu còn khác hiện tại (R18). */
    boolean outdated(ApprovalRequestView request, ResidentOpinion opinion) {
        return !opinion.contentDigest()
                .equals(ResidencyChangeDigest.of(request.content(), request.requestedEffectiveDate()));
    }

    /**
     * DBR-07: tạo phụ lục, khóa thuộc tính (FR-046); đổi giường thì giữ giường theo lịch ở 003 (FR-041). Không phải thay
     * đổi nhỏ thì phụ lục ở Chờ ký tới khi Hành chính ghi nhận bản đã ký (FR-043d, Q-420); thay đổi nhỏ thì Chờ hiệu lực.
     */
    @Override
    public void onApprove(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        boolean minor = isMinor(request);
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
                time.now(),
                !minor,
                minor ? Addendum.Status.PENDING_EFFECTIVE : Addendum.Status.PENDING_SIGNATURE);
        addenda.addAttributes(
                request.id(), contract.id(), contract.residentId(), change.changeTypes(), request.requesterId());
        try {
            addenda.setPending(request.id(), true);
        } catch (DuplicateKeyException e) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "DUPLICATE_PENDING_ATTRIBUTE",
                    "Đã có yêu cầu khác được duyệt và đang chờ tới ngày hiệu lực với cùng nội dung thay đổi này.")));
        }
        if (change.afterUrgentTransfer()) {
            beds.getObject().urgentTransferRequestResult(change.urgentTransferAllocationId(), true);
        } else if (change.firstBed(baseType(base))) {
            // 003 mục 1.4a: bán trú sang nội trú, giữ giường bằng phân bổ Tương lai đầu tiên từ ngày hiệu lực
            beds.getObject()
                    .reserveFirstAllocation(
                            request.id(),
                            contract.residentId(),
                            change.newBedId(),
                            actualEffectiveDate,
                            ResidencyChangeRules.firstBedExpectedEnd(change),
                            ResidencyChangeRules.residencyType(change.residenceType()));
        } else if (change.has(ChangeType.BED) && change.newBedId() != null) {
            beds.getObject()
                    .reserveScheduledTransfer(
                            request.id(), contract.residentId(), change.newBedId(), actualEffectiveDate);
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
     * Ký phụ lục muộn (FR-043d, Q-420): giữ giường đặt lúc duyệt (chuyển theo lịch, hoặc phân bổ đầu tiên của người bán
     * trú sang nội trú) dời sang ngày hiệu lực mới, cùng logic và điều kiện như {@link #onApprove}; không đạt thì 409 liệt
     * kê điều kiện, cả lệnh rollback. Sau chuyển gấp thì không có giữ giường nên bỏ qua.
     */
    void moveReservation(ApprovalRequestView request, LocalDate newDate) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        if (change.afterUrgentTransfer() || change.newBedId() == null) {
            return;
        }
        Contract contract = contracts.find(change.contractId()).orElseThrow(NotFoundException::new);
        boolean first = change.firstBed(baseType(base(contract.id()).orElseThrow()));
        if (!first && !change.has(ChangeType.BED)) {
            return;
        }
        BedAllocationCommandApi api = beds.getObject();
        api.cancelScheduledAllocation(
                request.id(), new com.eldercare.backend.common.Reason("Ký phụ lục muộn, dời ngày hiệu lực"));
        List<UnmetCondition> unmet = first
                ? api.checkFirstAllocationReservation(
                        contract.residentId(),
                        change.newBedId(),
                        newDate,
                        ResidencyChangeRules.firstBedExpectedEnd(change),
                        ResidencyChangeRules.residencyType(change.residenceType()))
                : api.checkScheduledReservation(contract.residentId(), change.newBedId(), newDate);
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        if (first) {
            api.reserveFirstAllocation(
                    request.id(),
                    contract.residentId(),
                    change.newBedId(),
                    newDate,
                    ResidencyChangeRules.firstBedExpectedEnd(change),
                    ResidencyChangeRules.residencyType(change.residenceType()));
        } else {
            api.reserveScheduledTransfer(request.id(), contract.residentId(), change.newBedId(), newDate);
        }
    }

    /** FR-043e (Q-419): người duyệt trả về Nháp thay vì sửa nội dung. */
    @Override
    public boolean allowReturnToDraft() {
        return true;
    }

    /** FR-043e: xác nhận người đại diện cũ bị bỏ (cả kèm bằng chứng và trên ứng dụng); người lập, Hành chính được báo. */
    @Override
    public void onReturnToDraft(ApprovalRequestView request, com.eldercare.backend.common.Reason reason) {
        confirmations.voidAll(request.id(), time.now());
        Long requesterStaff = accounts.getObject()
                .byId(request.requesterId())
                .filter(a -> a.type() != AccountType.FAMILY_MEMBER)
                .map(a -> a.staffId())
                .orElse(null);
        events.publishEvent(new ResidencyChangeReturnedToDraft(
                request.id(), request.target().residentId(), requesterStaff, reason.value(), time.now()));
    }

    /**
     * FR-043f (Q-421): Nháp, Chờ duyệt do người lập, Hành chính, Quản lý viện hủy; tài khoản người thân chỉ hủy yêu cầu do
     * chính mình lập. Đã duyệt vẫn do người duyệt (000).
     */
    @Override
    public boolean mayCancel(CurrentUser user, ApprovalRequestView request) {
        if (request.requesterId() == user.accountId()) {
            return true;
        }
        return user.type() != AccountType.FAMILY_MEMBER
                && (user.roles().contains(Role.OFFICE_STAFF) || user.roles().contains(Role.NURSING_HOME_MANAGER));
    }

    /** FR-043g (Q-422): gửi duyệt yêu cầu liên quan tới gia đình thì người đại diện khác được báo. */
    @Override
    public void onSubmit(ApprovalRequestView submitted) {
        if (!familyRelated(submitted)) {
            return;
        }
        ResidencyChangeContent change = ResidencyChangeContent.parse(submitted.content());
        events.publishEvent(new ResidencyChangeSubmittedNotice(
                submitted.id(),
                submitted.target().residentId(),
                names(change.changeTypes()),
                submitted.requestedEffectiveDate(),
                confirmerAccount(submitted),
                time.now()));
    }

    private static final Map<ChangeType, String> TYPE_LABELS = Map.of(
            ChangeType.RESIDENCE_TYPE, "Loại lưu trú",
            ChangeType.CARE_LEVEL, "Mức chăm sóc",
            ChangeType.SERVICE, "Dịch vụ",
            ChangeType.DURATION, "Thời hạn",
            ChangeType.BED, "Loại phòng",
            ChangeType.ARRIVAL_SCHEDULE, "Lịch đến");

    /**
     * Thông báo gửi duyệt tới Quản lý viện nêu người cao tuổi, số hợp đồng, loại thay đổi, ngày hiệu lực mong muốn (chủ dự
     * án yêu cầu 2026-11-21: người duyệt biết yêu cầu của cụ nào, hợp đồng nào). Không nêu giá trị mới, giá.
     */
    @Override
    public Optional<String> submittedNoticeSummary(ApprovalRequestView submitted) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(submitted.content());
        String name =
                residents.getObject().identity(submitted.target().residentId()).fullName();
        String number = contracts
                .find(change.contractId())
                .map(Contract::contractNumber)
                .orElse(null);
        String types =
                change.changeTypes().stream().sorted().map(TYPE_LABELS::get).collect(Collectors.joining(", "));
        return Optional.of("Điều chỉnh hợp đồng " + (number == null ? "" : number + " ") + "của " + name + ": "
                + types + "; ngày hiệu lực mong muốn "
                + "%02d/%02d/%d"
                        .formatted(
                                submitted.requestedEffectiveDate().getDayOfMonth(),
                                submitted.requestedEffectiveDate().getMonthValue(),
                                submitted.requestedEffectiveDate().getYear())
                + ".");
    }

    /**
     * Liên quan tới gia đình (FR-043g): không phải thay đổi nhỏ, hoặc do người đại diện lập qua cổng. Thay đổi nhỏ do viện tự
     * quyết thì không báo người đại diện.
     */
    boolean familyRelated(ApprovalRequestView request) {
        return !isMinor(request)
                || accounts.getObject()
                        .byId(request.requesterId())
                        .map(a -> a.type() == AccountType.FAMILY_MEMBER)
                        .orElse(false);
    }

    /** Báo người đại diện kết quả (FR-043g), khi yêu cầu liên quan tới gia đình. */
    void outcome(ApprovalRequestView request, ResidencyChangeOutcomeNotice.Outcome outcome) {
        if (familyRelated(request)) {
            events.publishEvent(new ResidencyChangeOutcomeNotice(
                    request.id(),
                    request.target().residentId(),
                    outcome,
                    names(ResidencyChangeContent.parse(request.content()).changeTypes()),
                    time.now()));
        }
    }

    /** Tài khoản người đại diện đã xác nhận: người đồng ý trên ứng dụng, hoặc người được ghi trong xác nhận kèm bằng chứng. */
    private Long confirmerAccount(ApprovalRequestView request) {
        Optional<Long> app = confirmations
                .latest(request.id())
                .filter(r ->
                        com.eldercare.backend.admission.internal.change.domain.ChangeConfirmationRequest.ACCEPTED
                                .equals(r.status()))
                .map(r -> r.content().respondedByAccountId());
        if (app.isPresent()) {
            return app.get();
        }
        return confirmations
                .validConfirmation(request.id(), request.content())
                .flatMap(c -> accounts.getObject().byFamilyMember(c.representativeFamilyMemberId()))
                .map(a -> a.accountId())
                .orElse(null);
    }

    /** FR-043d (Q-420): phụ lục còn Chờ ký thì chưa áp dụng; yêu cầu giữ Đã duyệt, không Áp dụng không thành. */
    @Override
    public boolean readyToApply(ApprovalRequestView request) {
        return addenda.findByRequest(request.id())
                .map(addendum -> addendum.status() != Addendum.Status.PENDING_SIGNATURE)
                .orElse(true);
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
            withdrawn(request);
            outcome(request, ResidencyChangeOutcomeNotice.Outcome.APPLY_FAILED);
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
        return rules.checkApply(contract, base.get().content(), change, request.actualEffectiveDate(), request.id());
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
            beds.getObject()
                    .extendExpectedEnd(
                            contract.residentId(),
                            newEnd.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant());
        }
        if ((change.has(ChangeType.BED) || change.firstBed(baseType(base)))
                && change.newBedId() != null
                && !change.afterUrgentTransfer()) {
            // Mốc dự kiến là 00:00 ngày hiệu lực; 003 ghi thời điểm thực hiện và dấu trễ (003 FR-016a)
            beds.getObject()
                    .applyScheduledTransfer(
                            request.id(),
                            effective.atStartOfDay(BusinessTime.ZONE).toInstant());
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
        events.publishEvent(new AddendumAppliedNotice(
                addendum.id(), contract.residentId(), names(change.changeTypes()), effective, time.now()));
        if (names(change.changeTypes()).contains("RESIDENCE_TYPE")) {
            beds.getObject().residenceTypeChanged(contract.residentId());
            events.publishEvent(new ResidenceTypeChanged(contract.residentId()));
        }
        return new ResultRef(ADDENDUM_ENTITY, String.valueOf(addendum.id()));
    }

    /** 003 FR-030: Hành chính lập yêu cầu sau chuyển gấp thì 003 gỡ dấu chờ lập yêu cầu, ngừng nhắc (cùng transaction lập). */
    void created(ResidencyChangeContent change, long requestId) {
        if (change.afterUrgentTransfer()) {
            beds.getObject().requestCreatedAfterUrgentTransfer(change.urgentTransferAllocationId(), requestId);
        }
    }

    /**
     * 003 FR-030 (chủ dự án chốt 2026-11-12): yêu cầu sau chuyển gấp bị hủy hoặc Áp dụng không thành, và không còn yêu cầu
     * khác (Nháp, Chờ duyệt, Đã duyệt, Đã áp dụng) tham chiếu cùng lần chuyển: 003 đặt lại dấu chờ lập yêu cầu và mốc nhắc.
     */
    void withdrawn(ApprovalRequestView request) {
        Long allocation = ResidencyChangeContent.parse(request.content()).urgentTransferAllocationId();
        if (allocation == null) {
            return;
        }
        boolean another = addenda.requestsOf(request.target().residentId()).stream()
                .filter(id -> id != request.id())
                .map(id -> approvals.getObject().find(id))
                .flatMap(Optional::stream)
                .filter(other -> TYPE.equals(other.type()) && LIVE.contains(other.status()))
                .anyMatch(other -> allocation.equals(
                        ResidencyChangeContent.parse(other.content()).urgentTransferAllocationId()));
        if (!another) {
            beds.getObject().urgentTransferRequestWithdrawn(allocation);
        }
    }

    private static final Set<String> LIVE = Set.of(
            ApprovalStatus.DRAFT, ApprovalStatus.PENDING_APPROVAL, ApprovalStatus.APPROVED, ApprovalStatus.APPLIED);

    /** 003 FR-030: yêu cầu sau chuyển gấp bị từ chối thì phân bổ mang dấu chờ chuyển về phòng cùng giá. */
    void rejected(ApprovalRequestView request) {
        ResidencyChangeContent change = ResidencyChangeContent.parse(request.content());
        if (change.afterUrgentTransfer()) {
            beds.getObject().urgentTransferRequestResult(change.urgentTransferAllocationId(), false);
        }
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

    private static ResidenceType baseType(ContractVersion base) {
        return ResidenceType.valueOf(base.content().get("residenceType").asString());
    }

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
        public Optional<Long> roomTypeOfBed(long bedId) {
            return structure.getObject().roomTypeOfBed(bedId).map(RoomTypeRef::id);
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
