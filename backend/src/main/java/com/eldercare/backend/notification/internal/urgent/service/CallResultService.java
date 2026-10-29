package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import com.eldercare.backend.notification.internal.recipient.service.RecipientPhones;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore.Reminder;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi kết quả gọi ({@code POST /call-reminders/{id}/results}; FR-029, FR-029a, FR-030, FR-031): chỉ người có quyền gọi;
 * Đã liên lạc được bắt buộc ghi chú, Không liên lạc được bắt buộc lý do. Lượt Chờ kết quả chuyển Đã có kết quả; sau "Không
 * liên lạc được" ghi thêm được lần gọi bổ sung. Đã liên lạc được: thông báo của chuỗi Đã xác nhận qua cuộc gọi. Không liên
 * lạc được: người thân thì nhắc gọi người thân kế tiếp chưa gọi trong chuỗi (thứ tự của 012), hết người hoặc người cần gọi
 * là nhân viên thì thông báo chuyển Không liên lạc được và báo Quản lý viện. Tự kiểm quyền (FR-029 theo nhiệm vụ lúc ghi).
 */
@Service
public class CallResultService {

    static final AuditAction RECORDED = new AuditAction("CALL_RESULT_RECORDED", false);
    static final String ENTITY = "M13.CALL_REMINDER";
    private static final Set<String> REASONS = Set.of("NO_ANSWER", "WRONG_NUMBER", "NO_NUMBER", "OTHER");

    /** Kết quả gọi người dùng nhập (schema {@code CallResult}). */
    public record CallResultInput(
            String result,
            Instant calledAt,
            Optional<String> calledPhoneNumber,
            Optional<String> reason,
            Optional<String> answeredBy,
            Optional<String> note) {}

    private final CallReminderStore store;
    private final CallReminderRouting routing;
    private final CallReminderActions actions;
    private final CallReminderNotices notices;
    private final RecipientPhones phones;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final ParameterApi parameters;
    private final BusinessTime time;

    CallResultService(
            CallReminderStore store,
            CallReminderRouting routing,
            CallReminderActions actions,
            CallReminderNotices notices,
            RecipientPhones phones,
            ObjectProvider<FamilyNotificationPort> family,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            ParameterApi parameters,
            BusinessTime time) {
        this.store = store;
        this.routing = routing;
        this.actions = actions;
        this.notices = notices;
        this.phones = phones;
        this.family = family;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.parameters = parameters;
        this.time = time;
    }

    @BusinessCommand(requiresReason = false)
    @Transactional
    public void record(long reminderId, CallResultInput in) {
        CurrentUser user = currentUser.getObject();
        Reminder r = store.lock(reminderId).orElseThrow(NotFoundException::new);
        if (!routing.canCall(user, r.floorAreaId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "NOT_ALLOWED_TO_CALL",
                    "Chỉ Người phụ trách ca, Trưởng tầng của tầng và Quản lý viện ghi kết quả gọi");
        }
        validate(in);
        boolean supplementary;
        if ("AWAITING_RESULT".equals(r.status())) {
            supplementary = false;
        } else if ("RESULT_RECORDED".equals(r.status()) && "NOT_REACHED".equals(r.finalResult())) {
            supplementary = true;
        } else {
            throw new BusinessException(HttpStatus.CONFLICT, "CALL_REMINDER_ENDED", "Lượt nhắc gọi đã kết thúc");
        }
        Instant now = time.now();
        String phone = in.calledPhoneNumber()
                .filter(p -> !p.isBlank())
                .or(() -> phones.of(r.calleeType(), r.calleeId(), r.residentId()))
                .orElse(null);
        long resultId = store.insertResult(
                r.id(),
                user.accountId(),
                in.calledAt(),
                phone,
                in.result(),
                in.reason().orElse(null),
                in.answeredBy().orElse(null),
                in.note().orElse(null),
                supplementary,
                now);
        audit(r, in, supplementary);
        boolean reached = "REACHED".equals(in.result());
        if (!supplementary || reached) {
            store.finish(r.id(), "RESULT_RECORDED", in.result(), null);
        }
        if (!supplementary && !user.staffId().equals(Optional.of(r.currentStaffId()))) {
            notices.inform(
                    r,
                    List.of(r.currentStaffId()),
                    "CALL_RESULT_RECORDED",
                    Long.toString(resultId),
                    "Lượt nhắc gọi bạn đang được nhắc đã có kết quả gọi do người khác ghi",
                    now);
        }
        if (reached) {
            actions.acknowledgeByCall(r.id(), user.accountId(), now);
        } else if (!supplementary) {
            notReached(r, now);
        }
    }

    /** FR-029a (a): người thân kế tiếp chưa gọi trong chuỗi; không còn ai thì hết chuỗi. */
    private void notReached(Reminder r, Instant now) {
        FamilyNotificationPort port = family.getIfAvailable();
        if (r.calleeType() == RecipientType.FAMILY_MEMBER && r.residentId() != null && port != null) {
            List<Long> called = store.chainFamilyCallees(r.chainId());
            Optional<Long> next = port.callOrder(r.residentId()).stream()
                    .filter(id -> !called.contains(id))
                    .findFirst();
            if (next.isPresent()) {
                String key = CallReminderCreation.calleeKey(
                        RecipientType.FAMILY_MEMBER, next.get(), r.residentId(), null, null);
                Optional<Long> pending = store.lockPending(key);
                if (pending.isPresent()) {
                    // Người thân kế tiếp đã có lượt Chờ kết quả về cùng người cao tuổi: gộp vào lượt đó (FR-032)
                    Reminder other = store.find(pending.get()).orElseThrow();
                    long handoff = store.handoff(
                            other.id(),
                            other.currentStaffId(),
                            other.currentStaffId(),
                            other.tier(),
                            "CONTENT_UPDATED",
                            other.dueAt(),
                            now);
                    notices.inform(
                            other,
                            List.of(other.currentStaffId()),
                            "CALL_REMINDER_UPDATED",
                            Long.toString(handoff),
                            "Lượt nhắc gọi có thêm nội dung cần báo",
                            now);
                    return;
                }
                Optional<CallReminderRouting.Assignment> to = routing.initial(r.floorAreaId(), Optional.empty(), now);
                if (to.isPresent()) {
                    long id = store.insertReminder(
                            r.chainId(),
                            RecipientType.FAMILY_MEMBER,
                            next.get(),
                            r.residentId(),
                            null,
                            null,
                            r.floorAreaId(),
                            "PREVIOUS_NOT_REACHED",
                            now);
                    Instant due =
                            now.plus(parameters.read(ParameterCode.CFG_M13_04).asDuration());
                    store.insertState(id, due, to.get().staffId(), key);
                    actions.assign(id, null, to.get(), "INITIAL", due, now);
                    return;
                }
            }
        }
        actions.chainNotReached(r, now);
    }

    private static void validate(CallResultInput in) {
        if (in.calledAt() == null || !Set.of("REACHED", "NOT_REACHED").contains(in.result())) {
            throw new InvalidValueException("Cần kết quả gọi (REACHED, NOT_REACHED) và thời điểm gọi");
        }
        if ("REACHED".equals(in.result()) && in.note().filter(n -> !n.isBlank()).isEmpty()) {
            throw new InvalidValueException("Đã liên lạc được cần ghi chú (FR-030)");
        }
        if ("NOT_REACHED".equals(in.result())
                && in.reason().filter(REASONS::contains).isEmpty()) {
            throw new InvalidValueException("Không liên lạc được cần lý do (FR-030)");
        }
    }

    private void audit(Reminder r, CallResultInput in, boolean supplementary) {
        Map<String, Object> after = new HashMap<>();
        after.put("result", in.result());
        after.put("calledAt", in.calledAt().toString());
        after.put("supplementary", supplementary);
        in.reason().ifPresent(v -> after.put("reason", v));
        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY, r.id())
                .resident(r.residentId())
                .after(after)
                .build());
    }
}
