package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.job.service.M13FastTask;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderTarget.Tier;
import com.eldercare.backend.notification.internal.urgent.domain.Reminder;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chuyển nhắc (việc 20 giây {@code HAND_OFF_CALL_REMINDERS}; FR-030a, research B-08): người được nhắc ở bậc Người phụ trách
 * ca không còn là Người phụ trách ca đang diễn ra của tầng (hết ca, vắng ca) thì nhắc người đang phụ trách ca, không có thì
 * người kế tiếp; quá hạn CFG-M13-04 chưa có kết quả thì nhắc người kế tiếp theo FR-029 và báo người trước; đã tới Quản lý
 * viện thì nhắc lại người đó mỗi CFG-M13-04 và báo các Quản lý viện khác. Mỗi lượt một transaction, hạn tính lại từ lần nhắc.
 */
@Service
public class CallReminderHandoffs implements M13FastTask {

    private static final Logger log = LoggerFactory.getLogger(CallReminderHandoffs.class);

    private final CallReminderStore store;
    private final CallReminderRouting routing;
    private final CallReminderActions actions;
    private final CallReminderNotices notices;
    private final ParameterApi parameters;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    CallReminderHandoffs(
            CallReminderStore store,
            CallReminderRouting routing,
            CallReminderActions actions,
            CallReminderNotices notices,
            ParameterApi parameters,
            TransactionTemplate tx,
            BusinessTime time) {
        this.store = store;
        this.routing = routing;
        this.actions = actions;
        this.notices = notices;
        this.parameters = parameters;
        this.tx = tx;
        this.time = time;
    }

    @Override
    public String name() {
        return "HAND_OFF_CALL_REMINDERS";
    }

    @Override
    public int run() {
        Instant now = time.now();
        int moved = 0;
        for (Reminder candidate : store.awaiting()) {
            try {
                Boolean done = tx.execute(s -> store.lock(candidate.id())
                        .filter(r -> "AWAITING_RESULT".equals(r.status()))
                        .map(r -> handOff(r, now))
                        .orElse(false));
                if (Boolean.TRUE.equals(done)) {
                    moved++;
                }
            } catch (RuntimeException e) {
                // Một lượt lỗi không chặn các lượt khác; nhịp sau xét lại
                log.warn("Chuyển nhắc lỗi", e);
            }
        }
        return moved;
    }

    private boolean handOff(Reminder r, Instant now) {
        Optional<Long> calleeStaff =
                r.calleeType() == RecipientType.STAFF ? Optional.of(r.calleeId()) : Optional.empty();
        Instant due = now.plus(parameters.read(ParameterCode.CFG_M13_04).asDuration());
        if (r.tier() == Tier.SHIFT_LEAD) {
            Optional<Long> lead =
                    routing.eligibleShiftLead(r.floorAreaId(), now).filter(id -> !calleeStaff.equals(Optional.of(id)));
            if (!lead.equals(Optional.of(r.currentStaffId()))) {
                Optional<CallReminderRouting.Assignment> to = lead.map(
                                id -> new CallReminderRouting.Assignment(id, Tier.SHIFT_LEAD, List.of()))
                        .or(() -> routing.after(Tier.SHIFT_LEAD, r.floorAreaId(), calleeStaff, now));
                return to.map(a -> move(r, a, "SHIFT_ENDED", due, now)).orElse(false);
            }
        }
        if (r.dueAt().isAfter(now)) {
            return false;
        }
        Optional<CallReminderRouting.Assignment> next = routing.after(r.tier(), r.floorAreaId(), calleeStaff, now);
        if (next.isEmpty()) {
            return false;
        }
        if (r.tier() == Tier.MANAGERS) {
            // Nhắc lại Quản lý viện đang giữ lượt nếu còn đủ điều kiện (FR-030a "Quản lý viện đó")
            List<Long> managers = routing.managerStaffIds(calleeStaff);
            long keep = managers.contains(r.currentStaffId())
                    ? r.currentStaffId()
                    : next.get().staffId();
            List<Long> others = managers.stream().filter(id -> id != keep).toList();
            return move(r, new CallReminderRouting.Assignment(keep, Tier.MANAGERS, others), "MANAGER_REPEAT", due, now);
        }
        return move(r, next.get(), "OVERDUE", due, now);
    }

    private boolean move(Reminder r, CallReminderRouting.Assignment to, String reason, Instant due, Instant now) {
        store.reassign(r.id(), to.staffId(), due);
        actions.assign(r.id(), r.currentStaffId(), to, reason, due, now);
        if (to.staffId() != r.currentStaffId()) {
            notices.inform(
                    r,
                    List.of(r.currentStaffId()),
                    "CALL_REMINDER_HANDED_OFF",
                    reason + ":" + now.toEpochMilli(),
                    "Việc nhắc gọi đã chuyển cho người khác; bạn vẫn ghi được kết quả nếu có quyền gọi",
                    now);
        }
        return true;
    }
}
