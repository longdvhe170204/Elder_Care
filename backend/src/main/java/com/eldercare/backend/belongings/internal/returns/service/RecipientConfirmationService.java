package com.eldercare.backend.belongings.internal.returns.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.CONTACT_ATTEMPT_RECORDED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RECIPIENT_CONFIRMATION_CANCELLED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RECIPIENT_CONFIRMATION_CONFIRMED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RECIPIENT_CONFIRMATION_CREATED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RECIPIENT_CONFIRMATION_ON_BEHALF;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RECIPIENT_CONFIRMATION_REJECTED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.RECIPIENT_CONFIRMATION_USED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.returns.domain.ContactAttempt;
import com.eldercare.backend.belongings.internal.returns.domain.ContactChannel;
import com.eldercare.backend.belongings.internal.returns.domain.ContactEvidence;
import com.eldercare.backend.belongings.internal.returns.domain.ContactEvidenceRule;
import com.eldercare.backend.belongings.internal.returns.domain.RecipientConfirmation;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xác nhận người nhận khác ({@code XacNhanNguoiNhanService}; 013 T018; FR-017, FR-017a, FR-017b, BR-M12-04, research
 * K-06): Hành chính lập, người đại diện xác nhận hoặc từ chối qua cổng, Quản lý viện duyệt thay khi đủ điều kiện, Hành
 * chính hủy; Hết hạn theo CFG-M10-08 và báo Quản lý viện khi đủ điều kiện ở {@link RecipientConfirmationTimers} (job M12
 * chờ A, T006). Người đại diện xét theo {@code FamilyRightsApi} tại thời điểm thao tác.
 */
@Service
public class RecipientConfirmationService {

    /** Tệp bản ký dùng chung loại chủ sở hữu với đồng ý cho tự giữ (Hành chính, Quản lý viện tải được). */
    private static final String SIGNED_FORM_OWNER = "M12.SIGNED_FORM";

    private static final EntityRef NO_PARENT = null;

    private static final Set<BelongingStatus> RETURNABLE =
            EnumSet.of(BelongingStatus.HELD, BelongingStatus.IN_USE, BelongingStatus.DAMAGED);

    private static final Set<String> OPEN =
            Set.of(RecipientConfirmation.PENDING_CONFIRMATION, RecipientConfirmation.EFFECTIVE);

    /** Căn cứ duyệt thay (api-013 {@code basis}): FR-017a (a) không liên hệ được, (b) không có người đại diện. */
    public enum Basis {
        REPRESENTATIVE_UNREACHABLE,
        NO_REPRESENTATIVE
    }

    /** Lệnh lập yêu cầu; {@code signedRepresentativeId}, {@code signedFormFileId} cùng có hoặc cùng trống. */
    public record CreateCommand(
            String recipientName,
            String recipientIdDocument,
            boolean recipientIsResident,
            List<Long> belongingIds,
            Long signedRepresentativeId,
            Long signedFormFileId) {}

    /** Một lần liên hệ do Hành chính ghi. */
    public record ContactCommand(Long familyMemberId, ContactChannel channel, Instant at, String result) {}

    /** Xác nhận để trả về qua REST (api-013 {@code RecipientConfirmation}). */
    public record View(
            long id,
            long residentId,
            String recipientName,
            String recipientIdDocument,
            boolean recipientIsResident,
            List<Long> belongingIds,
            String status,
            String method,
            Instant expiresAt) {}

    private enum Ground {
        A,
        B,
        NONE
    }

    private final SimpleRecordApi records;
    private final RecordEventApi recordEvents;
    private final BelongingStore belongings;
    private final BelongingResidentLock residentLock;
    private final FamilyRightsApi family;
    private final FileApi files;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher publisher;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    RecipientConfirmationService(
            SimpleRecordApi records,
            RecordEventApi recordEvents,
            BelongingStore belongings,
            BelongingResidentLock residentLock,
            FamilyRightsApi family,
            FileApi files,
            ParameterApi parameters,
            AuditLogApi audit,
            ApplicationEventPublisher publisher,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.records = records;
        this.recordEvents = recordEvents;
        this.belongings = belongings;
        this.residentLock = residentLock;
        this.family = family;
        this.files = files;
        this.parameters = parameters;
        this.audit = audit;
        this.publisher = publisher;
        this.time = time;
        this.users = users;
    }

    // ---- đọc ----

    /** Người cao tuổi của yêu cầu, để dựng phạm vi quyền của lệnh trên mã (404 khi không có). */
    public long residentOf(long id) {
        return records.require(RecipientConfirmation.KIND, id).residentId();
    }

    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.VIEW)
    public List<View> list(Target resident, Set<String> statuses) {
        return records.listByResident(RecipientConfirmation.KIND, resident.residentId(), statuses).stream()
                .map(this::view)
                .toList();
    }

    /** Không lọc người cao tuổi: chỉ Hành chính, Quản lý viện (FR-029). */
    public List<View> listAll(Set<String> statuses) {
        Set<Role> roles = users.getObject().roles();
        if (!roles.contains(Role.OFFICE_STAFF) && !roles.contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException(
                    "Chỉ Hành chính, Quản lý viện xem mọi yêu cầu xác nhận người nhận khác");
        }
        return records.listByStatus(RecipientConfirmation.KIND, statuses.isEmpty() ? OPEN : statuses).stream()
                .map(this::view)
                .toList();
    }

    // ---- lập ----

    /** Hành chính lập; kèm bản ký của người đại diện thì Hiệu lực ngay (FR-017). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public long create(Target resident, CreateCommand command) {
        CurrentUser user = requireOffice();
        long residentId = resident.residentId();
        if (blank(command.recipientName())
                || blank(command.recipientIdDocument())
                || command.belongingIds() == null
                || command.belongingIds().isEmpty()
                || command.belongingIds().stream().distinct().count()
                        != command.belongingIds().size()
                || (command.signedRepresentativeId() == null) != (command.signedFormFileId() == null)) {
            throw invalid("Thiếu người nhận, giấy tờ, danh sách đồ hoặc bản ký không đủ cặp người đại diện và tệp");
        }
        residentLock.lock(residentId);
        for (long belongingId : command.belongingIds()) {
            BelongingView belonging = belongings.find(belongingId).orElseThrow(NotFoundException::new);
            if (belonging.residentId() != residentId) {
                throw invalid("Đồ gửi không thuộc người cao tuổi của yêu cầu");
            }
            if (!RETURNABLE.contains(belonging.status())) {
                throw blocked("INVALID_STATE", "Chỉ lập cho đồ Đang giữ, Đang được sử dụng hoặc Hư hỏng");
            }
        }
        Instant now = time.now();
        boolean signed = command.signedRepresentativeId() != null;
        if (signed && !family.isRepresentative(command.signedRepresentativeId(), residentId, now)) {
            throw blocked("GIVER_NOT_REPRESENTATIVE", "Người ký không phải người đại diện tại thời điểm ký");
        }
        RecipientConfirmation content = new RecipientConfirmation(
                command.recipientName().strip(),
                command.recipientIdDocument().strip(),
                command.recipientIsResident(),
                List.copyOf(command.belongingIds()),
                List.of(),
                user.accountId(),
                user.staffId().orElseThrow(),
                now,
                signed ? RecipientConfirmation.SIGNED_FORM : null,
                command.signedRepresentativeId(),
                command.signedFormFileId(),
                null,
                signed ? command.signedRepresentativeId() : null,
                signed ? now : null,
                signed ? now.plus(validity()) : null,
                null,
                null,
                null,
                null);
        var created = records.create(
                RecipientConfirmation.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        NO_PARENT,
                        signed ? RecipientConfirmation.EFFECTIVE : RecipientConfirmation.PENDING_CONFIRMATION,
                        content,
                        null,
                        null,
                        null,
                        user.accountId(),
                        false));
        if (signed) {
            files.attach(command.signedFormFileId(), SIGNED_FORM_OWNER, created.id());
        }
        audit.write(AuditLogEntry.builder()
                .action(RECIPIENT_CONFIRMATION_CREATED)
                .entity(RecipientConfirmation.KIND.code(), created.id())
                .resident(residentId)
                .after(Map.of(
                        "recipientName",
                        content.recipientName(),
                        "belongingIds",
                        content.belongingIds(),
                        "signedForm",
                        signed))
                .build());
        if (!signed) {
            notice(
                    "RECIPIENT_CONFIRMATION_REQUESTED",
                    created.id(),
                    NotificationLevel.MEDIUM,
                    residentId,
                    List.of(new RecipientGroup.Representative(residentId)),
                    "Có yêu cầu xác nhận người nhận đồ gửi",
                    user.accountId(),
                    now);
            notifyManagerIfEligible(created.id(), residentId, now);
        }
        return created.id();
    }

    /** Hành chính ghi một lần liên hệ người đại diện (FR-017a). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public void recordContact(Target resident, long id, ContactCommand command) {
        CurrentUser user = requireOffice();
        if (command.familyMemberId() == null
                || command.channel() == null
                || command.at() == null
                || blank(command.result())) {
            throw invalid("Lần liên hệ cần người đại diện, kênh, thời điểm và kết quả");
        }
        SimpleRecord<RecipientConfirmation> confirmation = lockedConfirmation(id);
        if (!RecipientConfirmation.PENDING_CONFIRMATION.equals(confirmation.status())) {
            throw blocked("INVALID_STATE", "Chỉ ghi bằng chứng liên hệ cho yêu cầu Chờ xác nhận");
        }
        Instant now = time.now();
        if (command.at().isAfter(now)) {
            throw invalid("Thời điểm liên hệ không được ở tương lai");
        }
        if (!family.isRepresentative(command.familyMemberId(), confirmation.residentId(), now)) {
            throw invalid("Chỉ ghi liên hệ với người đại diện Hiệu lực của người cao tuổi");
        }
        recordEvents.add(
                ContactAttempt.KIND,
                new EntityRef(RecipientConfirmation.KIND.code(), id),
                new NewRecordEvent<>(
                        new ContactAttempt(
                                command.familyMemberId(),
                                command.channel(),
                                command.at(),
                                command.result().strip(),
                                user.accountId()),
                        null,
                        user.accountId(),
                        false,
                        command.at(),
                        null,
                        now,
                        null,
                        null));
        audit.write(AuditLogEntry.builder()
                .action(CONTACT_ATTEMPT_RECORDED)
                .entity(RecipientConfirmation.KIND.code(), id)
                .resident(confirmation.residentId())
                .after(Map.of(
                        "familyMemberId",
                        command.familyMemberId(),
                        "channel",
                        command.channel().name()))
                .build());
        notifyManagerIfEligible(id, confirmation.residentId(), now);
    }

    // ---- người đại diện qua cổng ----

    @Transactional
    public void confirmOnPortal(long id) {
        long representativeId = familyMember();
        SimpleRecord<RecipientConfirmation> confirmation = lockedConfirmation(id);
        requireRepresentative(representativeId, confirmation.residentId());
        requirePending(confirmation);
        Instant now = time.now();
        records.change(
                RecipientConfirmation.KIND,
                id,
                confirmation.version(),
                new RecordChange<>(
                        RecipientConfirmation.EFFECTIVE,
                        confirmation
                                .content()
                                .effective(
                                        RecipientConfirmation.PORTAL,
                                        users.getObject().accountId(),
                                        representativeId,
                                        now,
                                        now.plus(validity()),
                                        null,
                                        null),
                        null,
                        null,
                        false));
        audit.write(AuditLogEntry.builder()
                .action(RECIPIENT_CONFIRMATION_CONFIRMED)
                .entity(RecipientConfirmation.KIND.code(), id)
                .resident(confirmation.residentId())
                .after(Map.of("method", RecipientConfirmation.PORTAL, "representativeId", representativeId))
                .build());
        notifyCreator(
                "RECIPIENT_CONFIRMATION_CONFIRMED",
                id,
                confirmation,
                "Yêu cầu xác nhận người nhận đã được xác nhận",
                now);
    }

    @Transactional
    public void rejectOnPortal(long id, String reason) {
        long representativeId = familyMember();
        Reason why = Reason.of(reason);
        SimpleRecord<RecipientConfirmation> confirmation = lockedConfirmation(id);
        requireRepresentative(representativeId, confirmation.residentId());
        requirePending(confirmation);
        Instant now = time.now();
        records.change(
                RecipientConfirmation.KIND,
                id,
                confirmation.version(),
                new RecordChange<>(
                        RecipientConfirmation.REJECTED,
                        confirmation.content().closed(users.getObject().accountId(), representativeId, why.value()),
                        null,
                        null,
                        true));
        audit.write(AuditLogEntry.builder()
                .action(RECIPIENT_CONFIRMATION_REJECTED)
                .entity(RecipientConfirmation.KIND.code(), id)
                .resident(confirmation.residentId())
                .before(Map.of("status", RecipientConfirmation.PENDING_CONFIRMATION))
                .after(Map.of("status", RecipientConfirmation.REJECTED))
                .reason(why)
                .build());
        notifyCreator(
                "RECIPIENT_CONFIRMATION_REJECTED", id, confirmation, "Yêu cầu xác nhận người nhận đã bị từ chối", now);
    }

    // ---- Quản lý viện duyệt thay ----

    /** FR-017a: chỉ Quản lý viện; khóa xem theo phạm vi, vai trò kiểm trong service (plan điểm 13). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.VIEW)
    @Transactional
    public void approveOnBehalf(Target resident, long id, String reason, Basis basis) {
        CurrentUser user = users.getObject();
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện duyệt thay xác nhận người nhận khác (FR-017a)");
        }
        Reason why = Reason.of(reason);
        if (basis == null) {
            throw invalid("Cần chọn căn cứ duyệt thay (FR-017a)");
        }
        SimpleRecord<RecipientConfirmation> confirmation = lockedConfirmation(id);
        if (RecipientConfirmation.REJECTED.equals(confirmation.status())) {
            throw blocked("ALREADY_REJECTED", "Người đại diện đã từ chối, Quản lý viện không duyệt thay (FR-017)");
        }
        requirePending(confirmation);
        if (confirmation.content().createdBy() == user.accountId()) {
            throw blocked("SELF_APPROVAL_NOT_ALLOWED", "Không tự lập rồi tự duyệt thay (FR-017a)");
        }
        Instant now = time.now();
        Ground ground = ground(confirmation, now);
        boolean eligible = basis == Basis.REPRESENTATIVE_UNREACHABLE ? ground == Ground.A : ground == Ground.B;
        if (!eligible) {
            throw blocked(
                    "ON_BEHALF_APPROVAL_NOT_ELIGIBLE",
                    "Chưa đủ điều kiện duyệt thay theo căn cứ đã chọn (FR-017a, CFG-M12-05, CFG-M12-06)");
        }
        records.change(
                RecipientConfirmation.KIND,
                id,
                confirmation.version(),
                new RecordChange<>(
                        RecipientConfirmation.EFFECTIVE,
                        confirmation
                                .content()
                                .effective(
                                        RecipientConfirmation.MANAGER_ON_BEHALF,
                                        user.accountId(),
                                        null,
                                        now,
                                        now.plus(validity()),
                                        basis.name(),
                                        why.value()),
                        null,
                        null,
                        false));
        audit.write(AuditLogEntry.builder()
                .action(RECIPIENT_CONFIRMATION_ON_BEHALF)
                .entity(RecipientConfirmation.KIND.code(), id)
                .resident(confirmation.residentId())
                .before(Map.of("status", RecipientConfirmation.PENDING_CONFIRMATION))
                .after(Map.of("status", RecipientConfirmation.EFFECTIVE, "basis", basis.name()))
                .reason(why)
                .build());
        List<RecipientGroup> representatives = representatives(confirmation.residentId());
        if (!representatives.isEmpty()) {
            notice(
                    "RECIPIENT_CONFIRMATION_APPROVED_ON_BEHALF",
                    id,
                    NotificationLevel.MEDIUM,
                    confirmation.residentId(),
                    representatives,
                    "Quản lý viện đã duyệt thay xác nhận người nhận đồ gửi",
                    user.accountId(),
                    now);
        }
        notifyCreator(
                "RECIPIENT_CONFIRMATION_CONFIRMED",
                id,
                confirmation,
                "Yêu cầu xác nhận người nhận đã được duyệt thay",
                now);
    }

    /** Hành chính hủy (có lý do). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public void cancel(Target resident, long id, String reason) {
        requireOffice();
        Reason why = Reason.of(reason);
        SimpleRecord<RecipientConfirmation> confirmation = lockedConfirmation(id);
        if (!OPEN.contains(confirmation.status())) {
            throw blocked("INVALID_STATE", "Yêu cầu không còn Chờ xác nhận hoặc Hiệu lực");
        }
        close(
                confirmation,
                RecipientConfirmation.CANCELLED,
                why,
                users.getObject().accountId());
    }

    // ---- dùng cho lệnh Trả ----

    /** Yêu cầu của người cao tuổi theo mã, khóa dòng để dùng trong lệnh Trả; trống khi không có hoặc của người khác. */
    @Transactional(propagation = Propagation.MANDATORY)
    public java.util.Optional<SimpleRecord<RecipientConfirmation>> lockForUse(long id, long residentId) {
        boolean present = records.listByResident(RecipientConfirmation.KIND, residentId, Set.of()).stream()
                .anyMatch(r -> r.id() == id);
        return present
                ? java.util.Optional.of(records.lockForUpdate(RecipientConfirmation.KIND, id))
                : java.util.Optional.empty();
    }

    /** Hiệu lực tại {@code now}: trạng thái Hiệu lực và chưa quá hạn (kiểm cả khi job Hết hạn chưa chạy). */
    public boolean isUsable(SimpleRecord<RecipientConfirmation> confirmation, Instant now) {
        return RecipientConfirmation.EFFECTIVE.equals(confirmation.status())
                && confirmation.content().expiresAt() != null
                && now.isBefore(confirmation.content().expiresAt());
    }

    /** Hiệu lực → Đã dùng trong lệnh Trả (FR-017b). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markUsed(SimpleRecord<RecipientConfirmation> confirmation, long handoverId) {
        records.change(
                RecipientConfirmation.KIND,
                confirmation.id(),
                confirmation.version(),
                new RecordChange<>(
                        RecipientConfirmation.USED, confirmation.content().used(handoverId), null, null, true));
        audit.write(AuditLogEntry.builder()
                .action(RECIPIENT_CONFIRMATION_USED)
                .entity(RecipientConfirmation.KIND.code(), confirmation.id())
                .resident(confirmation.residentId())
                .after(Map.of("handoverId", handoverId))
                .build());
    }

    // ---- việc của hệ thống (job M12, T006) ----

    /** Hiệu lực quá CFG-M10-08 chưa dùng → Hết hạn, báo người lập (FR-017b). Trả {@code false} khi không còn cần. */
    @Transactional
    public boolean expire(long id) {
        long residentId = residentOf(id);
        residentLock.lock(residentId);
        SimpleRecord<RecipientConfirmation> confirmation = records.lockForUpdate(RecipientConfirmation.KIND, id);
        Instant now = time.now();
        if (!RecipientConfirmation.EFFECTIVE.equals(confirmation.status())
                || confirmation.content().expiresAt() == null
                || now.isBefore(confirmation.content().expiresAt())) {
            return false;
        }
        records.change(
                RecipientConfirmation.KIND,
                id,
                confirmation.version(),
                new RecordChange<>(
                        RecipientConfirmation.EXPIRED,
                        confirmation.content().closed(null, null, "Hết hiệu lực"),
                        null,
                        null,
                        true));
        notifyCreator("RECIPIENT_CONFIRMATION_EXPIRED", id, confirmation, "Xác nhận người nhận đã hết hạn", now);
        return true;
    }

    /** Chờ xác nhận mà đủ điều kiện duyệt thay (FR-017a (a) hoặc (b)) → báo Quản lý viện một lần. */
    @Transactional
    public void notifyEligible(long id) {
        long residentId = residentOf(id);
        residentLock.lock(residentId);
        SimpleRecord<RecipientConfirmation> confirmation = records.lockForUpdate(RecipientConfirmation.KIND, id);
        if (RecipientConfirmation.PENDING_CONFIRMATION.equals(confirmation.status())) {
            notifyManagerIfEligible(id, residentId, time.now());
        }
    }

    /**
     * Đồ rời Đang giữ, Đang được sử dụng, Hư hỏng thì bị loại khỏi danh sách các yêu cầu còn mở; hết đồ thì yêu cầu tự
     * Hủy "không còn đồ" (FR-017b).
     */
    @Transactional
    public void dropBelonging(long belongingId) {
        BelongingView belonging = belongings.find(belongingId).orElse(null);
        if (belonging == null || RETURNABLE.contains(belonging.status())) {
            return;
        }
        residentLock.lock(belonging.residentId());
        for (SimpleRecord<RecipientConfirmation> listed :
                records.listByResident(RecipientConfirmation.KIND, belonging.residentId(), OPEN)) {
            if (!listed.content().belongingIds().contains(belongingId)
                    || listed.content().excludedBelongingIds().contains(belongingId)) {
                continue;
            }
            SimpleRecord<RecipientConfirmation> confirmation =
                    records.lockForUpdate(RecipientConfirmation.KIND, listed.id());
            if (!OPEN.contains(confirmation.status())) {
                continue;
            }
            RecipientConfirmation dropped = confirmation.content().withExcluded(belongingId);
            Instant now = time.now();
            if (dropped.remaining().isEmpty()) {
                records.change(
                        RecipientConfirmation.KIND,
                        confirmation.id(),
                        confirmation.version(),
                        new RecordChange<>(
                                RecipientConfirmation.CANCELLED,
                                dropped.closed(null, null, "Không còn đồ trong danh sách"),
                                null,
                                null,
                                true));
                notifyCreator(
                        "RECIPIENT_CONFIRMATION_CANCELLED_NO_ITEMS",
                        confirmation.id(),
                        confirmation,
                        "Yêu cầu xác nhận người nhận đã tự hủy vì không còn đồ",
                        now);
            } else {
                records.change(
                        RecipientConfirmation.KIND,
                        confirmation.id(),
                        confirmation.version(),
                        new RecordChange<>(confirmation.status(), dropped, null, null, false));
            }
        }
    }

    /** Mã các yêu cầu theo trạng thái, cho việc của hệ thống. */
    public List<Long> idsOf(String status) {
        return records.listByStatus(RecipientConfirmation.KIND, Set.of(status)).stream()
                .map(SimpleRecord::id)
                .toList();
    }

    // ---- nội bộ ----

    private void close(SimpleRecord<RecipientConfirmation> confirmation, String status, Reason why, long accountId) {
        records.change(
                RecipientConfirmation.KIND,
                confirmation.id(),
                confirmation.version(),
                new RecordChange<>(
                        status, confirmation.content().closed(accountId, null, why.value()), null, null, true));
        audit.write(AuditLogEntry.builder()
                .action(RECIPIENT_CONFIRMATION_CANCELLED)
                .entity(RecipientConfirmation.KIND.code(), confirmation.id())
                .resident(confirmation.residentId())
                .before(Map.of("status", confirmation.status()))
                .after(Map.of("status", status))
                .reason(why)
                .build());
    }

    private SimpleRecord<RecipientConfirmation> lockedConfirmation(long id) {
        long residentId = residentOf(id);
        residentLock.lock(residentId);
        return records.lockForUpdate(RecipientConfirmation.KIND, id);
    }

    private void requirePending(SimpleRecord<RecipientConfirmation> confirmation) {
        if (!RecipientConfirmation.PENDING_CONFIRMATION.equals(confirmation.status())) {
            throw blocked("INVALID_STATE", "Yêu cầu không còn Chờ xác nhận");
        }
    }

    /** Căn cứ duyệt thay hiện có (FR-017a): (b) không có người đại diện Hiệu lực; (a) chờ quá CFG-M12-05 và đủ bằng chứng. */
    private Ground ground(SimpleRecord<RecipientConfirmation> confirmation, Instant now) {
        List<Long> representatives = family.representatives(confirmation.residentId()).stream()
                .map(FamilyMemberView::familyMemberId)
                .toList();
        if (representatives.isEmpty()) {
            return Ground.B;
        }
        Duration wait = parameters.read(ParameterCode.CFG_M12_05).asDuration();
        if (now.isBefore(confirmation.content().createdAt().plus(wait))) {
            return Ground.NONE;
        }
        ContactEvidenceRule rule = parameters.read(ParameterCode.CFG_M12_06).asTable(ContactEvidenceRule.class);
        List<ContactEvidence.Attempt> attempts =
                recordEvents
                        .list(ContactAttempt.KIND, new EntityRef(RecipientConfirmation.KIND.code(), confirmation.id()))
                        .stream()
                        .map(e -> new ContactEvidence.Attempt(
                                e.content().familyMemberId(),
                                e.content().channel(),
                                e.content().result()))
                        .toList();
        return ContactEvidence.sufficient(representatives, attempts, rule.attemptsPerRepresentative(), rule.channels())
                ? Ground.A
                : Ground.NONE;
    }

    /** Báo Quản lý viện một lần khi yêu cầu Chờ xác nhận đủ điều kiện duyệt thay (FR-032). */
    private void notifyManagerIfEligible(long id, long residentId, Instant now) {
        SimpleRecord<RecipientConfirmation> confirmation = records.lockForUpdate(RecipientConfirmation.KIND, id);
        if (confirmation.content().managerNotifiedAt() != null || ground(confirmation, now) == Ground.NONE) {
            return;
        }
        records.change(
                RecipientConfirmation.KIND,
                id,
                confirmation.version(),
                new RecordChange<>(
                        confirmation.status(), confirmation.content().managerNotified(now), null, null, false));
        notice(
                "RECIPIENT_CONFIRMATION_ELIGIBLE",
                id,
                NotificationLevel.MEDIUM,
                residentId,
                List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                "Yêu cầu xác nhận người nhận đủ điều kiện để Quản lý viện duyệt thay",
                null,
                now);
    }

    private List<RecipientGroup> representatives(long residentId) {
        List<RecipientGroup> groups = new ArrayList<>();
        for (FamilyMemberView member : family.representatives(residentId)) {
            groups.add(new RecipientGroup.FamilyMember(member.familyMemberId(), "REPRESENTATIVE"));
        }
        return groups;
    }

    private void notifyCreator(
            String eventType, long id, SimpleRecord<RecipientConfirmation> confirmation, String text, Instant now) {
        notice(
                eventType,
                id,
                NotificationLevel.LOW,
                confirmation.residentId(),
                List.of(new RecipientGroup.Staff(confirmation.content().createdByStaffId(), "CREATOR")),
                text,
                null,
                now);
    }

    private void notice(
            String eventType,
            long id,
            NotificationLevel level,
            long residentId,
            List<RecipientGroup> recipients,
            String text,
            Long causedBy,
            Instant now) {
        publisher.publishEvent(BelongingNotices.notice(
                eventType, RecipientConfirmation.KIND.code(), id, level, residentId, recipients, text, causedBy, now));
    }

    private View view(SimpleRecord<RecipientConfirmation> record) {
        RecipientConfirmation content = record.content();
        return new View(
                record.id(),
                record.residentId(),
                content.recipientName(),
                content.recipientIdDocument(),
                content.recipientIsResident(),
                content.remaining(),
                record.status(),
                content.method(),
                content.expiresAt());
    }

    private Duration validity() {
        return parameters.read(ParameterCode.CFG_M10_08).asDuration();
    }

    private CurrentUser requireOffice() {
        CurrentUser user = users.getObject();
        if (!user.roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính lập, ghi bằng chứng, hủy xác nhận người nhận khác");
        }
        return user;
    }

    private long familyMember() {
        CurrentUser user = users.getObject();
        if (user.type() != AccountType.FAMILY_MEMBER || user.familyMemberId().isEmpty()) {
            throw new PermissionDeniedException("Chỉ tài khoản người thân dùng cổng");
        }
        return user.familyMemberId().get();
    }

    private void requireRepresentative(long familyMemberId, long residentId) {
        if (!family.isRepresentative(familyMemberId, residentId, time.now())) {
            throw new PermissionDeniedException("Chỉ người đại diện hiện tại xác nhận, từ chối (FR-017)");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }
}
