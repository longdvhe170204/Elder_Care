package com.eldercare.backend.belongings.internal.returns.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RETURN_BLOCKED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.domain.ConditionHandling;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.domain.NewHandover;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnBasis;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnBlockReason;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnFacts;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingLocks;
import com.eldercare.backend.belongings.internal.belonging.service.ValuableTypes;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.returns.domain.RecipientConfirmation;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnCommand;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
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

/**
 * Phần có transaction của lệnh Trả ({@code TraService}; 013 T018): khóa, xác minh người nhận ({@link ReturnRecipient}),
 * ghi lần chặn trả hoặc thực hiện Trả. Lần chặn là kết quả trả về, không ném lỗi, để transaction commit cùng bản ghi
 * chặn rồi {@link ReturnService} mới ném lỗi (FR-016, SC-004; plan điểm 13).
 */
@Service
public class ReturnTransaction {

    private static final String SIGNATURE_OWNER = "M12.RETURN_SIGNATURE";
    private static final String RECORD_TYPE = "M12.BELONGING_HANDOVER";

    /** Kết quả: đã trả, hoặc bị chặn (lỗi chờ ném sau commit). */
    public record Outcome(BelongingHandoverService.Result result, BusinessException blocked) {}

    private final BelongingLocks locks;
    private final BelongingHandoverService handovers;
    private final BelongingHandoverStore handoverStore;
    private final RecipientConfirmationService confirmations;
    private final ValuableTypes valuableTypes;
    private final CatalogApi catalogs;
    private final FamilyRightsApi family;
    private final ResidentApi residents;
    private final FileApi files;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher publisher;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    ReturnTransaction(
            BelongingLocks locks,
            BelongingHandoverService handovers,
            BelongingHandoverStore handoverStore,
            RecipientConfirmationService confirmations,
            ValuableTypes valuableTypes,
            CatalogApi catalogs,
            FamilyRightsApi family,
            ResidentApi residents,
            FileApi files,
            AuditLogApi audit,
            ApplicationEventPublisher publisher,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.locks = locks;
        this.handovers = handovers;
        this.handoverStore = handoverStore;
        this.confirmations = confirmations;
        this.valuableTypes = valuableTypes;
        this.catalogs = catalogs;
        this.family = family;
        this.residents = residents;
        this.files = files;
        this.audit = audit;
        this.publisher = publisher;
        this.time = time;
        this.users = users;
    }

    @Transactional
    public Outcome run(Target resident, ReturnCommand command) {
        CurrentUser user = users.getObject();
        user.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên trả đồ gửi"));
        validate(command);
        long residentId = resident.residentId();
        Map<Long, Integer> versions = new LinkedHashMap<>();
        for (ReturnCommand.Item item : command.items()) {
            if (versions.put(item.belongingId(), item.version()) != null) {
                throw invalid("Một đồ chỉ xuất hiện một lần trong lệnh");
            }
        }
        List<LockedBelonging> locked = locks.lock(residentId, versions);
        Instant now = time.now();
        boolean office = user.roles().contains(Role.OFFICE_STAFF);

        List<ReturnRecipient.Item> items = new ArrayList<>();
        for (LockedBelonging b : locked) {
            var type = catalogs.require(BelongingCatalogs.BELONGING_TYPE, b.typeId());
            items.add(new ReturnRecipient.Item(
                    b.id(),
                    valuableTypes.isValuable(type.name()),
                    type.attributes().requiresConsentForUse()));
        }
        ReturnCommand.Recipient recipient = command.recipient();
        ReturnRecipient.Declared declared = new ReturnRecipient.Declared(
                recipient.type(),
                recipient.familyMemberId(),
                recipient.fullName(),
                recipient.documentType(),
                recipient.documentNumber());
        Optional<SimpleRecord<RecipientConfirmation>> confirmation = command.recipientConfirmationId() == null
                ? Optional.empty()
                : confirmations.lockForUse(command.recipientConfirmationId(), residentId);
        ReturnRecipient.Result verdict = ReturnRecipient.check(
                declared,
                items,
                familyFacts(recipient, residentId, now),
                confirmation.map(c -> new ReturnRecipient.Confirmation(
                        confirmations.isUsable(c, now),
                        c.content().recipientIsResident(),
                        c.content().recipientName(),
                        c.content().recipientIdDocument(),
                        c.content().remaining())),
                office);
        if (!verdict.allowed()) {
            return new Outcome(null, recordBlock(user, residentId, locked, command, verdict.blockReason(), now));
        }

        ReturnFacts facts = new ReturnFacts(
                recipient.documentType(),
                recipient.documentNumber(),
                verdict.basis(),
                verdict.verification(),
                command.signatureFileId(),
                command.noSignatureReason(),
                command.conditionOpinion(),
                verdict.basis() == ReturnBasis.RECIPIENT_CONFIRMATION ? command.recipientConfirmationId() : null,
                null);
        // 013-2 (a), FR-012: trả một phần (tiền mặt) đi qua phần tách của lệnh bàn giao, phần trả là đồ mới Đã trả
        List<HandoverCommand.Item> handoverItems = command.items().stream()
                .map(i -> new HandoverCommand.Item(
                        i.belongingId(),
                        i.version(),
                        i.amount(),
                        null,
                        i.conditionLevelId(),
                        i.conditionDescription(),
                        i.photoFileIds(),
                        null))
                .toList();
        // FR-015: lúc trả ghi tình trạng quan sát được, không bắt chọn xử lý khi xấu hơn (Đã trả là trạng thái cuối)
        HandoverCommand handover = new HandoverCommand(
                HandoverType.RETURN,
                handoverItems,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                ConditionHandling.ACKNOWLEDGE,
                null);
        Long recipientId =
                recipient.type() == GiverType.RESIDENT ? Long.valueOf(residentId) : recipient.familyMemberId();
        BelongingHandoverService.Result result = handovers.returnItems(
                resident,
                handover,
                new BelongingHandoverService.ReturnInfo(recipient.type(), recipientId, recipient.fullName(), facts));
        long first = result.handovers().getFirst().handoverId();
        if (command.signatureFileId() != null) {
            files.attach(command.signatureFileId(), SIGNATURE_OWNER, first);
        }
        if (verdict.basis() == ReturnBasis.RECIPIENT_CONFIRMATION) {
            confirmations.markUsed(confirmation.orElseThrow(), first);
        }
        afterReturn(user, residentId, first, recipient, verdict, now);
        return new Outcome(result, null);
    }

    private void validate(ReturnCommand command) {
        ReturnCommand.Recipient recipient = command.recipient();
        if (command.items().isEmpty()
                || recipient == null
                || recipient.type() == null
                || recipient.type() == GiverType.STAFF
                || (recipient.type() == GiverType.FAMILY_MEMBER && recipient.familyMemberId() == null)
                || (recipient.type() != GiverType.RESIDENT && blank(recipient.fullName()))
                || command.items().stream().anyMatch(i -> i.conditionLevelId() == null)) {
            throw invalid("Thiếu đồ, tình trạng lúc trả hoặc người nhận (họ tên; mã người thân với người thân)");
        }
        boolean unsigned = command.signatureFileId() == null;
        // FR-019: người nhận ký trên phiếu trả; người cao tuổi không còn khả năng ký thì nhân viên ghi lý do
        if (unsigned && (recipient.type() != GiverType.RESIDENT || blank(command.noSignatureReason()))) {
            throw invalid("Phiếu trả cần bản ký của người nhận (FR-019)");
        }
    }

    private ReturnRecipient.FamilyFacts familyFacts(ReturnCommand.Recipient recipient, long residentId, Instant now) {
        if (recipient.type() != GiverType.FAMILY_MEMBER) {
            return ReturnRecipient.FamilyFacts.none();
        }
        long memberId = recipient.familyMemberId();
        boolean representative = family.isRepresentative(memberId, residentId, now);
        List<FamilyMemberView> pickers = family.allowedPickersAt(residentId, now);
        boolean picker = pickers.stream().anyMatch(p -> p.familyMemberId() == memberId);
        String profileName = java.util.stream.Stream.concat(
                        pickers.stream(), family.representatives(residentId).stream())
                .filter(p -> p.familyMemberId() == memberId)
                .map(FamilyMemberView::fullName)
                .findFirst()
                .orElse(null);
        return new ReturnRecipient.FamilyFacts(
                representative,
                picker,
                profileName,
                family.idDocument(memberId).map(d -> new ReturnRecipient.RegisteredDocument(d.type(), d.number())));
    }

    /** Ghi một bản ghi chặn mỗi đồ trong lệnh (cùng commit với kết quả trả về) rồi dựng lỗi để ném sau commit. */
    private BusinessException recordBlock(
            CurrentUser user,
            long residentId,
            List<LockedBelonging> locked,
            ReturnCommand command,
            ReturnBlockReason reason,
            Instant now) {
        ReturnCommand.Recipient recipient = command.recipient();
        GiverType recipientType = recipient.type();
        Long recipientId = recipientType == GiverType.RESIDENT ? Long.valueOf(residentId) : recipient.familyMemberId();
        long staffId = user.staffId().orElseThrow();
        Set<Long> recorded = new HashSet<>();
        for (LockedBelonging b : locked) {
            var type = catalogs.require(BelongingCatalogs.BELONGING_TYPE, b.typeId());
            long id = handoverStore.insert(new NewHandover(
                    b.id(),
                    residentId,
                    HandoverType.RETURN_BLOCKED,
                    b.status(),
                    GiverType.STAFF,
                    staffId,
                    null,
                    null,
                    recipientType,
                    recipientId,
                    recipient.fullName(),
                    now,
                    b.quantity(),
                    b.cashAmount(),
                    b.conditionLevelId(),
                    null,
                    null,
                    null,
                    false,
                    valuableTypes.isValuable(type.name()),
                    List.of(),
                    null,
                    user.accountId(),
                    now,
                    new ReturnFacts(
                            recipient.documentType(),
                            recipient.documentNumber(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            command.recipientConfirmationId(),
                            reason)));
            recorded.add(id);
            audit.write(AuditLogEntry.builder()
                    .action(RETURN_BLOCKED)
                    .entity(RECORD_TYPE, id)
                    .resident(residentId)
                    .after(Map.of("belongingId", b.id(), "blockReason", reason.name()))
                    .build());
        }
        boolean invalidConfirmation = reason == ReturnBlockReason.INVALID_RECIPIENT_CONFIRMATION;
        boolean nursing = reason == ReturnBlockReason.NURSING_STAFF_VALUABLE_ITEM;
        String code = nursing
                ? "NURSING_STAFF_VALUABLE_ITEM"
                : invalidConfirmation ? "INVALID_RECIPIENT_CONFIRMATION" : "RETURN_BLOCKED";
        String message = switch (reason) {
            case ID_DOCUMENT_MISMATCH -> "Giấy tờ xuất trình không khớp hồ sơ người thân (FR-016)";
            case NOT_ENTITLED_TO_RECEIVE -> "Người nhận không có quyền nhận đồ (FR-016)";
            case INVALID_RECIPIENT_CONFIRMATION -> "Xác nhận người nhận khác không hợp lệ (FR-017)";
            case NURSING_STAFF_VALUABLE_ITEM -> "Điều dưỡng không trả đồ có giá trị; chặn cả lần trả (FR-016a)";
        };
        return new BusinessException(
                HttpStatus.CONFLICT, code, message, Map.of("blockReason", reason.name(), "blockedRecordIds", recorded));
    }

    private void afterReturn(
            CurrentUser user,
            long residentId,
            long firstHandoverId,
            ReturnCommand.Recipient recipient,
            ReturnRecipient.Result verdict,
            Instant now) {
        boolean deceased = residents.status(residentId) == ResidentStatus.DECEASED;
        List<RecipientGroup> representatives = new ArrayList<>();
        for (FamilyMemberView member : family.representatives(residentId)) {
            boolean receiver = recipient.type() == GiverType.FAMILY_MEMBER
                    && recipient.familyMemberId() != null
                    && recipient.familyMemberId() == member.familyMemberId();
            if (!receiver) {
                representatives.add(new RecipientGroup.FamilyMember(member.familyMemberId(), "REPRESENTATIVE"));
            }
        }
        if (!representatives.isEmpty()) {
            publisher.publishEvent(BelongingNotices.notice(
                    "BELONGING_RETURNED",
                    RECORD_TYPE,
                    firstHandoverId,
                    "BELONGING_RETURNED:" + firstHandoverId,
                    NotificationLevel.LOW,
                    residentId,
                    representatives,
                    "Đồ gửi đã được trả",
                    user.accountId(),
                    now,
                    deceased));
        }
        if (verdict.missingRegisteredDocument()) {
            publisher.publishEvent(BelongingNotices.notice(
                    "FAMILY_ID_DOCUMENT_MISSING",
                    RECORD_TYPE,
                    firstHandoverId,
                    NotificationLevel.LOW,
                    residentId,
                    List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                    "Hồ sơ người thân nhận đồ chưa có giấy tờ, cần bổ sung",
                    user.accountId(),
                    now));
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }
}
