package com.eldercare.backend.billing.internal.purchase.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.billing.api.dto.PurchaseAssistanceStatus;
import com.eldercare.backend.billing.api.events.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.period.service.CostNotifications;
import com.eldercare.backend.billing.internal.purchase.domain.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T027: lệnh mua hộ dùng khóa/version hồ sơ chung và lịch sử chỉ thêm trong cùng transaction. */
@Service
@Transactional
public class PurchaseAssistanceService {
    private final SimpleRecordApi records;
    private final RecordEventApi history;
    private final CostAccess access;
    private final ParameterApi parameters;
    private final RepresentativePort representatives;
    private final ObjectProvider<FamilyNotificationPort> families;
    private final ObjectProvider<PurchaseBalancePort> balances;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final CostNotifications notifications;
    private final UnitPriceApi catalog;

    public PurchaseAssistanceService(
            SimpleRecordApi records,
            RecordEventApi history,
            CostAccess access,
            ParameterApi parameters,
            RepresentativePort representatives,
            ObjectProvider<FamilyNotificationPort> families,
            ObjectProvider<PurchaseBalancePort> balances,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events,
            CostNotifications notifications,
            UnitPriceApi catalog) {
        this.records = records;
        this.history = history;
        this.access = access;
        this.parameters = parameters;
        this.representatives = representatives;
        this.families = families;
        this.balances = balances;
        this.audit = audit;
        this.time = time;
        this.events = events;
        this.notifications = notifications;
        this.catalog = catalog;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.CREATE)
    public long create(
            long resident,
            Long catalogItem,
            String description,
            BigDecimal quantity,
            long estimate,
            String requesterType,
            long requester,
            Reason reason) {
        long actor = office(Operation.CREATE, 0, resident);
        if (resident <= 0
                || requester <= 0
                || quantity == null
                || quantity.signum() <= 0
                || quantity.scale() > 3
                || quantity.compareTo(new BigDecimal("999999999.999")) > 0
                || requesterType == null
                || !Set.of("STAFF", "FAMILY_MEMBER").contains(requesterType)
                || reason == null
                || description == null
                || description.isBlank()) throw new InvalidValueException("Thông tin đề nghị chưa hợp lệ");
        if (catalogItem != null && catalog.service(catalogItem).isEmpty())
            throw new InvalidValueException("Vật phẩm không có trong danh mục");
        var content = new PurchaseDetails(
                catalogItem,
                description,
                quantity,
                reason.value(),
                requesterType,
                requester,
                PurchaseConsent.draft(estimate),
                0,
                null,
                false,
                null,
                null,
                null,
                null);
        var row = records.create(
                PurchaseDetails.KIND,
                new NewSimpleRecord<>(resident, null, "DRAFT", content, null, null, null, actor, false));
        log("PURCHASE_ASSISTANCE_CREATE", row, null, reason);
        return row.id();
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.UPDATE)
    public void send(long id) {
        var row = officeRow(id);
        var next = transition(() -> row.content()
                .consent()
                .send(parameters.read(ParameterCode.CFG_M11_02).asMoney()));
        save(
                row,
                row.content().withConsent(next, 1, time.now(), insufficient(row.residentId(), next.estimatedAmount())),
                "SEND",
                null);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.VIEW)
    public void agree(long id, Target target) {
        var row = decisionRow(id);
        var next = transition(() -> row.content().consent().agree());
        save(
                row,
                row.content()
                        .withConsent(
                                next,
                                row.content().round(),
                                row.content().sentAt(),
                                insufficient(row.residentId(), next.allowedAmount())),
                "AGREE",
                null);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.VIEW)
    public void reject(long id, Reason reason, Target target) {
        var row = currentBalance(decisionRow(id));
        var next = transition(() -> row.content().consent().reject());
        save(
                row,
                row.content()
                        .withConsent(
                                next,
                                row.content().round(),
                                row.content().sentAt(),
                                row.content().insufficientBalance()),
                "REJECT",
                reason);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.UPDATE)
    public void requestAdditional(long id, long amount, Reason reason) {
        var row = officeRow(id);
        var next = transition(() -> row.content().consent().requestAdditional(amount));
        save(
                row,
                row.content()
                        .withConsent(
                                next, row.content().round() + 1, time.now(), insufficient(row.residentId(), amount)),
                "REQUEST_ADDITIONAL",
                reason);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.UPDATE)
    public void withdraw(long id, Reason reason) {
        var row = officeRow(id);
        var next = transition(() -> row.content().consent().withdraw());
        save(
                row,
                row.content()
                        .withConsent(
                                next,
                                row.content().round(),
                                row.content().sentAt(),
                                row.content().insufficientBalance()),
                "WITHDRAW_ADDITIONAL",
                reason);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.UPDATE)
    public void cancel(long id, Reason reason) {
        var row = officeRow(id);
        var next = transition(() -> row.content().consent().cancel());
        save(
                row,
                row.content()
                        .withConsent(
                                next,
                                row.content().round(),
                                row.content().sentAt(),
                                row.content().insufficientBalance()),
                "CANCEL",
                reason);
    }

    @Transactional(readOnly = true)
    public Target target(long id) {
        var row = records.require(PurchaseDetails.KIND, id);
        return new Target(PurchaseDetails.KIND.code(), Long.toString(id), row.residentId(), null);
    }

    @Transactional(readOnly = true)
    public List<SimpleRecord<PurchaseDetails>> list(Long resident, String status) {
        Set<String> statuses;
        try {
            statuses = status == null
                    ? Set.of("DRAFT", "AWAITING_CONSENT", "PURCHASE_ALLOWED", "REJECTED", "CANCELLED", "PURCHASED")
                    : Set.of(PurchaseAssistanceStatus.valueOf(status).name());
        } catch (IllegalArgumentException invalid) {
            throw new InvalidValueException("Trạng thái đề nghị không hợp lệ");
        }
        if (access.user().type() != AccountType.FAMILY_MEMBER)
            access.viewOrCommand(Function.PURCHASE_ON_BEHALF, Operation.VIEW, PurchaseDetails.KIND.code(), 0, resident);
        return (resident == null
                        ? records.listByStatus(PurchaseDetails.KIND, statuses)
                        : records.listByResident(PurchaseDetails.KIND, resident, statuses))
                .stream()
                        .filter(row -> {
                            if (access.user().type() == AccountType.FAMILY_MEMBER && !eligible(row)) return false;
                            try {
                                access.viewOrCommand(
                                        Function.PURCHASE_ON_BEHALF,
                                        Operation.VIEW,
                                        PurchaseDetails.KIND.code(),
                                        row.id(),
                                        row.residentId());
                                return true;
                            } catch (PermissionDeniedException denied) {
                                return false;
                            }
                        })
                        .map(this::currentBalance)
                        .toList();
    }

    private SimpleRecord<PurchaseDetails> currentBalance(SimpleRecord<PurchaseDetails> row) {
        if (!row.status().equals("AWAITING_CONSENT")) return row;
        var c = row.content();
        long amount = c.consent().additionalPending()
                ? c.consent().additionalAmount()
                : c.consent().estimatedAmount();
        var content = c.withConsent(c.consent(), c.round(), c.sentAt(), insufficient(row.residentId(), amount));
        return new SimpleRecord<>(
                row.id(),
                row.residentId(),
                row.parent(),
                row.status(),
                content,
                row.effectiveFrom(),
                row.effectiveTo(),
                row.uniqueKey(),
                row.lockedAt(),
                row.createdAt(),
                row.createdBy(),
                row.version());
    }

    @Transactional(readOnly = true)
    public List<PurchaseDecision> decisions(long id) {
        var row = records.require(PurchaseDetails.KIND, id);
        access.viewOrCommand(
                Function.PURCHASE_ON_BEHALF, Operation.VIEW, PurchaseDetails.KIND.code(), id, row.residentId());
        if (access.user().type() == AccountType.FAMILY_MEMBER && !eligible(row))
            throw new PermissionDeniedException("Không có quyền xem đề nghị này");
        return history.list(PurchaseDecision.KIND, parent(id)).stream()
                .map(RecordEvent::content)
                .toList();
    }

    private boolean eligible(SimpleRecord<PurchaseDetails> row) {
        var user = access.user();
        if (user.familyMemberId().isEmpty() || !row.status().equals("AWAITING_CONSENT")) return false;
        long member = user.familyMemberId().orElseThrow();
        return representatives.isActiveRepresentative(member, row.residentId(), time.today())
                && history.list(PurchaseRecipient.KIND, parent(row.id())).stream()
                        .anyMatch(e -> e.content().round() == row.content().round()
                                && e.content().familyMemberId() == member);
    }

    private SimpleRecord<PurchaseDetails> officeRow(long id) {
        var row = records.require(PurchaseDetails.KIND, id);
        office(Operation.UPDATE, id, row.residentId());
        return records.lockForUpdate(PurchaseDetails.KIND, id);
    }

    private SimpleRecord<PurchaseDetails> decisionRow(long id) {
        var row = records.lockForUpdate(PurchaseDetails.KIND, id);
        if (access.user().type() == AccountType.FAMILY_MEMBER) {
            access.viewOrCommand(
                    Function.PURCHASE_ON_BEHALF, Operation.UPDATE, PurchaseDetails.KIND.code(), id, row.residentId());
            if (!row.status().equals("AWAITING_CONSENT"))
                throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_PROCESSED", "Đề nghị đã được xử lý");
            if (!eligible(row)) throw new PermissionDeniedException("Không còn quyền quyết định đề nghị này");
        } else
            access.require(
                    Role.NURSING_HOME_MANAGER,
                    Function.PURCHASE_ON_BEHALF,
                    Operation.APPROVE,
                    PurchaseDetails.KIND.code(),
                    id,
                    row.residentId());
        return row;
    }

    private long office(Operation op, long id, Long resident) {
        return access.require(
                Role.OFFICE_STAFF, Function.PURCHASE_ON_BEHALF, op, PurchaseDetails.KIND.code(), id, resident);
    }

    private boolean insufficient(long resident, long amount) {
        var port = balances.getIfAvailable();
        return port != null && port.insufficientBalance(resident, amount, time.now());
    }

    private void save(SimpleRecord<PurchaseDetails> before, PurchaseDetails content, String command, Reason reason) {
        if (Set.of("REJECT", "REQUEST_ADDITIONAL", "WITHDRAW_ADDITIONAL", "CANCEL")
                        .contains(command)
                && reason == null) throw new ReasonRequiredException();
        boolean sending = command.equals("SEND") || command.equals("REQUEST_ADDITIONAL");
        if (sending && content.consent().status().equals("AWAITING_CONSENT"))
            snapshotRecipients(before, content.round());
        var after = records.change(
                        PurchaseDetails.KIND,
                        before.id(),
                        before.version(),
                        new RecordChange<>(
                                content.consent().status(),
                                content,
                                null,
                                null,
                                content.consent().terminal()))
                .after();
        if (Set.of("AGREE", "REJECT", "REQUEST_ADDITIONAL", "WITHDRAW_ADDITIONAL")
                .contains(command)) {
            var user = access.user();
            var decision = new PurchaseDecision(
                    command,
                    before.content().consent().allowedAmount(),
                    command.equals("REQUEST_ADDITIONAL")
                            ? content.consent().additionalAmount()
                            : content.consent().allowedAmount(),
                    user.type().name(),
                    user.accountId(),
                    user.type() == AccountType.FAMILY_MEMBER
                            ? "FAMILY_PORTAL"
                            : user.roles().contains(Role.NURSING_HOME_MANAGER)
                                    ? "NURSING_HOME_MANAGER"
                                    : "OFFICE_STAFF",
                    reason == null ? null : reason.value(),
                    content.insufficientBalance(),
                    time.now());
            history.add(
                    PurchaseDecision.KIND,
                    parent(before.id()),
                    new NewRecordEvent<>(
                            decision, null, user.accountId(), false, time.now(), null, time.now(), null, null));
        }
        log("PURCHASE_ASSISTANCE_" + command, after, before, reason);
        var recipients = new ArrayList<RecipientGroup>();
        if (sending && after.status().equals("AWAITING_CONSENT")) {
            recipients.add(new RecipientGroup.Representative(after.residentId()));
            recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        } else {
            recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
            if (command.equals("CANCEL") || command.equals("WITHDRAW_ADDITIONAL"))
                recipients.add(new RecipientGroup.Representative(after.residentId()));
        }
        var notices = new ArrayList<NotificationRequest>();
        for (int index = 0; index < recipients.size(); index++) {
            var recipient = recipients.get(index);
            var level = sending
                            && after.status().equals("AWAITING_CONSENT")
                            && recipient instanceof RecipientGroup.Representative
                    ? NotificationLevel.MEDIUM
                    : NotificationLevel.LOW;
            notices.add(notifications.request(
                    command,
                    PurchaseDetails.KIND.code(),
                    after.id(),
                    after.residentId(),
                    "purchase:" + after.id() + ":" + after.version() + ":" + index,
                    "Đề nghị mua hộ #" + after.id() + ": " + label(command),
                    List.of(recipient),
                    level,
                    time.now(),
                    access.user().accountId()));
        }
        var status = PurchaseAssistanceStatus.valueOf(after.status());
        if (sending)
            events.publishEvent(new PurchaseAssistanceSent(
                    after.id(), after.residentId(), content.consent().estimatedAmount(), status, notices));
        else if (command.equals("CANCEL"))
            events.publishEvent(new PurchaseAssistanceCancelled(
                    after.id(), after.residentId(), content.consent().estimatedAmount(), status, notices));
        else
            events.publishEvent(new PurchaseAssistanceDecided(
                    after.id(), after.residentId(), content.consent().estimatedAmount(), status, notices));
    }

    private void snapshotRecipients(SimpleRecord<PurchaseDetails> row, int round) {
        var source = families.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn người đại diện");
        var members = source.activeRelationships(row.residentId()).stream()
                .filter(r -> r.representative())
                .toList();
        for (var member : members)
            history.add(
                    PurchaseRecipient.KIND,
                    parent(row.id()),
                    new NewRecordEvent<>(
                            new PurchaseRecipient(round, member.familyMemberId()),
                            null,
                            access.user().accountId(),
                            false,
                            time.now(),
                            null,
                            time.now(),
                            null,
                            "M11.RECIPIENT:" + row.id() + ":" + round + ":" + member.familyMemberId()));
        if (members.stream().noneMatch(m -> m.portalAccountId().isPresent() && m.portalAccountActive()))
            notifications.staff(
                    "PURCHASE_NO_PORTAL_REPRESENTATIVE",
                    PurchaseDetails.KIND.code(),
                    row.id(),
                    row.residentId(),
                    "purchase-no-portal:" + row.id() + ":" + round,
                    "Không có người đại diện dùng cổng; Quản lý viện quyết định đề nghị mua hộ",
                    Role.OFFICE_STAFF,
                    time.now(),
                    access.user().accountId());
    }

    private void log(
            String action, SimpleRecord<PurchaseDetails> after, SimpleRecord<PurchaseDetails> before, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(PurchaseDetails.KIND.code(), after.id())
                .resident(after.residentId())
                .before(before == null ? null : Map.of("status", before.status(), "content", before.content()))
                .after(Map.of("status", after.status(), "content", after.content()))
                .reason(reason)
                .build());
    }

    private static EntityRef parent(long id) {
        return new EntityRef(PurchaseDetails.KIND.code(), id);
    }

    private static PurchaseConsent transition(java.util.function.Supplier<PurchaseConsent> action) {
        try {
            return action.get();
        } catch (IllegalStateException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_PROCESSED", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new InvalidValueException(ex.getMessage());
        }
    }

    private static String label(String command) {
        return switch (command) {
            case "SEND" -> "đã gửi";
            case "AGREE" -> "đã đồng ý";
            case "REJECT" -> "đã từ chối";
            case "REQUEST_ADDITIONAL" -> "cần đồng ý bổ sung";
            case "WITHDRAW_ADDITIONAL" -> "đã rút yêu cầu bổ sung";
            case "CANCEL" -> "đã hủy";
            default -> throw new IllegalArgumentException("Lệnh không hợp lệ");
        };
    }
}
