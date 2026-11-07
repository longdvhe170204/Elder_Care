package com.eldercare.backend.belongings.internal.ending.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.UNCLAIMED_DISPOSAL_APPLIED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import com.eldercare.backend.belongings.internal.belonging.service.ValuableTypes;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.Contact;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.DisposalItem;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.Method;
import com.eldercare.backend.belongings.internal.ending.persistence.BelongingEndingStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu phê duyệt {@code M12.UNCLAIMED_BELONGING_DISPOSAL} (tên logic {@code M12.XU_LY_DO_KHONG_NGUOI_NHAN}; 013
 * T024, FR-027a, Q-152, Q-157, Q-159, BR-M12-08; research K-09). Hành chính lập, Quản lý viện duyệt, không tự duyệt
 * ({@code choTuDuyet = false} của contracts là {@link #allowSelfApproval()}). Nội dung: {@code residentId, belongingIds,
 * method (SELL|DESTROY|TRANSFER_TO_AUTHORITY), receivingUnit, minutesFileId, photoFileIds, contactAttempts[{familyMemberId,
 * channel, at, result}]}; lý do là lý do của yêu cầu. Khi áp dụng, mỗi đồ vẫn Đang giữ hoặc Hư hỏng có một bản ghi
 * {@code DISPOSAL}; đồ đã đổi trạng thái trong lúc chờ bị loại khỏi phần áp dụng (000 FR-038).
 */
@Component
class UnclaimedDisposalHandler implements RequestTypeHandler {

    static final String TYPE = "M12.UNCLAIMED_BELONGING_DISPOSAL";

    private static final String ENTITY = "M12.BELONGING";
    private static final Set<Role> APPROVERS = Set.of(Role.NURSING_HOME_MANAGER);

    /** Giá trị CFG-M12-07 (Q-157): số lần liên hệ tối thiểu mỗi người; khoảng cách lấy từ CFG-M12-02. */
    record EvidenceRule(int attempts, String minGapParameter) {}

    private final BelongingStore belongings;
    private final BelongingEndingStore endingStore;
    private final BelongingHandoverService handovers;
    private final ValuableTypes valuableTypes;
    private final ResidentApi residents;
    private final FamilyRightsApi family;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    UnclaimedDisposalHandler(
            BelongingStore belongings,
            BelongingEndingStore endingStore,
            BelongingHandoverService handovers,
            ValuableTypes valuableTypes,
            ResidentApi residents,
            FamilyRightsApi family,
            ParameterApi parameters,
            AuditLogApi audit,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.belongings = belongings;
        this.endingStore = endingStore;
        this.handovers = handovers;
        this.valuableTypes = valuableTypes;
        this.residents = residents;
        this.family = family;
        this.parameters = parameters;
        this.audit = audit;
        this.time = time;
        this.users = users;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    /** Hành chính lập; cấu trúc sai thì 422, vi phạm điều kiện của FR-027a thì 409 kèm mã. */
    @Override
    public void validateContent(JsonNode content) {
        if (!users.getObject().roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính lập đề nghị xử lý đồ không người nhận (FR-027a)");
        }
        long residentId = residentId(content);
        Method method = method(content);
        List<Long> ids = belongingIds(content);
        Set<Long> photos = new LinkedHashSet<>(ids(content.get("photoFileIds")));
        List<DisposalItem> items = new ArrayList<>();
        Map<Long, Instant> clocks = endingStore.clockStarts(ids);
        for (long id : ids) {
            BelongingView b = belongings.find(id).orElseThrow(NotFoundException::new);
            if (b.residentId() != residentId) {
                throw new InvalidValueException("Đồ không thuộc người cao tuổi của đề nghị");
            }
            items.add(new DisposalItem(
                    id,
                    b.status(),
                    b.cashAmount() != null,
                    valuableTypes.isValuableType(b.typeId()),
                    !photos.isEmpty(),
                    clocks.get(id)));
        }
        Instant now = time.now();
        Duration gap = parameters.read(ParameterCode.CFG_M12_02).asDuration();
        EvidenceRule rule = parameters.read(ParameterCode.CFG_M12_07).asTable(EvidenceRule.class);
        List<Long> people = new ArrayList<>(family.representatives(residentId).stream()
                .map(FamilyMemberView::familyMemberId)
                .toList());
        family.primaryContact(residentId)
                .map(FamilyMemberView::familyMemberId)
                .filter(id -> !people.contains(id))
                .ifPresent(people::add);
        List<String> violations = UnclaimedDisposalRules.violations(new UnclaimedDisposalRules.Check(
                now,
                parameters.read(ParameterCode.CFG_M12_03).asDuration(),
                residents.finalStatusAt(residentId).orElse(null),
                method,
                items,
                content.hasNonNull("minutesFileId"),
                people,
                contacts(content),
                rule.attempts(),
                gap));
        if (!violations.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    violations.getFirst(),
                    "Đề nghị xử lý đồ không người nhận chưa đạt điều kiện: " + String.join(", ", violations),
                    Map.of("violations", violations));
        }
    }

    /** Hành chính lập: khóa "Đồ gửi" thao tác Tạo (Phụ lục 27 chú thích). */
    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.BELONGINGS, Operation.CREATE);
    }

    /** AC-01 (Quản lý viện) chỉ có "Xem" ở dòng Đồ gửi: duyệt đi kèm giới hạn vai trò ở {@link #approverRoleRestriction}. */
    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.BELONGINGS, Operation.VIEW);
    }

    @Override
    public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return Optional.of(APPROVERS);
    }

    @Override
    public Target scope(JsonNode content) {
        return new Target(ENTITY, null, residentId(content), null);
    }

    /** Chưa còn đồ nào Đang giữ hoặc Hư hỏng thì không còn gì để áp dụng (FR-027a, 000 FR-038). */
    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        boolean anyLeft = belongingIds(request.content()).stream()
                .map(belongings::find)
                .flatMap(Optional::stream)
                .anyMatch(b -> b.status() == BelongingStatus.HELD || b.status() == BelongingStatus.DAMAGED);
        return anyLeft
                ? List.of()
                : List.of(new UnmetCondition(
                        "NO_DISPOSABLE_BELONGING",
                        "Mọi đồ trong đề nghị đã được trả hoặc đổi trạng thái nên không còn gì để áp dụng (FR-027a)"));
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        long residentId = residentId(request.content());
        String method = method(request.content()).name();
        String unit = text(request.content(), "receivingUnit");
        List<Long> applied = new ArrayList<>();
        List<Long> excluded = new ArrayList<>();
        Long firstRecord = null;
        for (long id : belongingIds(request.content())) {
            Optional<BelongingHandoverService.Created> created = handovers.dispose(
                    residentId,
                    id,
                    new BelongingHandoverService.Disposal(request.id(), method, unit, request.reason()));
            if (created.isPresent()) {
                applied.add(id);
                firstRecord = firstRecord == null ? created.get().handoverId() : firstRecord;
            } else {
                excluded.add(id);
            }
        }
        audit.write(AuditLogEntry.builder()
                .action(UNCLAIMED_DISPOSAL_APPLIED)
                .entity("M12.BELONGING_DISPOSAL_REQUEST", request.id())
                .resident(residentId)
                .after(Map.of("method", method, "applied", applied, "excluded", excluded))
                .basis(new Basis(null, request.id(), null, null))
                .build());
        return new ResultRef("M12.BELONGING_HANDOVER", String.valueOf(firstRecord));
    }

    /** Hành chính lập đề nghị chưa có quyền tự duyệt: người yêu cầu không được duyệt đề nghị của mình (FR-027a). */
    @Override
    public boolean allowSelfApproval() {
        return false;
    }

    private static long residentId(JsonNode content) {
        JsonNode value = content == null ? null : content.get("residentId");
        if (value == null || !value.isIntegralNumber() || value.asLong() <= 0) {
            throw new InvalidValueException("Thiếu người cao tuổi của đề nghị");
        }
        return value.asLong();
    }

    private static List<Long> belongingIds(JsonNode content) {
        List<Long> ids = ids(content == null ? null : content.get("belongingIds"));
        if (ids.isEmpty()) {
            throw new InvalidValueException("Đề nghị cần ít nhất một đồ gửi");
        }
        return ids;
    }

    private static List<Long> ids(JsonNode array) {
        List<Long> ids = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> {
                if (!n.isIntegralNumber()) {
                    throw new InvalidValueException("Mã không hợp lệ");
                }
                ids.add(n.asLong());
            });
        }
        return ids;
    }

    private static Method method(JsonNode content) {
        try {
            return Method.valueOf(text(content, "method"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Hình thức xử lý phải là SELL, DESTROY hoặc TRANSFER_TO_AUTHORITY");
        }
    }

    private static String text(JsonNode content, String field) {
        JsonNode value = content == null ? null : content.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static List<Contact> contacts(JsonNode content) {
        List<Contact> contacts = new ArrayList<>();
        JsonNode array = content.get("contactAttempts");
        if (array != null && array.isArray()) {
            for (JsonNode n : array) {
                try {
                    contacts.add(new Contact(
                            n.get("familyMemberId").asLong(),
                            Instant.parse(n.get("at").asString()),
                            text(n, "result")));
                } catch (DateTimeException | NullPointerException e) {
                    throw new InvalidValueException("Lần liên hệ thiếu người thân hoặc thời điểm");
                }
            }
        }
        return contacts;
    }
}
