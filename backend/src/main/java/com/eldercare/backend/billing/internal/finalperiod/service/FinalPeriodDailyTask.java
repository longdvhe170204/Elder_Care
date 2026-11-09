package com.eldercare.backend.billing.internal.finalperiod.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.billing.internal.finalperiod.persistence.FinalPeriodStore;
import com.eldercare.backend.billing.internal.period.domain.*;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-035: chỉ nhắc khi quá E, không tự lập khoản hoặc bảng. */
@Service
public class FinalPeriodDailyTask implements CostTimedTask {
    private final FinalPeriodStore store;
    private final CostLock locks;
    private final ResidentModuleStateApi states;
    private final ResidentApi residents;
    private final CostNotifications notices;
    private final ReminderMarkers markers;
    private final com.eldercare.backend.billing.internal.period.persistence.CostStatementStore statements;
    private final com.eldercare.backend.administration.parameter.api.ParameterApi parameters;

    public FinalPeriodDailyTask(
            FinalPeriodStore store,
            CostLock locks,
            ResidentModuleStateApi states,
            ResidentApi residents,
            CostNotifications notices,
            ReminderMarkers markers,
            com.eldercare.backend.billing.internal.period.persistence.CostStatementStore statements,
            com.eldercare.backend.administration.parameter.api.ParameterApi parameters) {
        this.store = store;
        this.locks = locks;
        this.states = states;
        this.residents = residents;
        this.notices = notices;
        this.markers = markers;
        this.statements = statements;
        this.parameters = parameters;
    }

    public String name() {
        return "COST_FINAL_PERIOD_OVERDUE";
    }

    public CostJobCadence cadence() {
        return CostJobCadence.DAILY;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int run(Instant slot) {
        var today = LocalDate.ofInstant(slot, BusinessTime.ZONE);
        int count = 0;
        for (long resident : store.trackedResidents()) {
            var state = states.get(BillingResidentState.KIND, resident).orElseGet(BillingResidentState::empty);
            if (state.finalStatementId() == null) continue;
            state = locks.resident(resident);
            if (state.finalStatementId() == null) continue;
            var cycle = parameters
                    .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M02_09)
                    .asDuration();
            for (var s : statements.list(null, resident, null)) {
                if (!s.statementType().equals("SUPPLEMENTARY") || s.status().equals("FINALIZED")) continue;
                if (store.lastSupplementReminder(s.id())
                        .filter(last -> last.plus(cycle).isAfter(slot))
                        .isPresent()) continue;
                if (markers.mark("M11.COST_REMINDER", "M11.COST_STATEMENT", s.id(), "SUPPLEMENTARY:" + slot, null)) {
                    notices.staff(
                            "SUPPLEMENTARY_COSTS_PENDING",
                            s.id(),
                            resident,
                            "M11.SUPPLEMENTARY:" + s.id() + ":" + slot,
                            "Còn bảng chi phí bổ sung chưa chốt",
                            Role.OFFICE_STAFF,
                            slot,
                            null);
                    count++;
                }
            }
            if (state.throughDate() == null
                    || !state.throughDate().isBefore(today)
                    || residents.isFinalStatus(resident)) continue;
            if (markers.mark(
                    "M11.COST_REMINDER",
                    "M11.COST_STATEMENT",
                    state.finalStatementId(),
                    "END_OVERDUE:" + today,
                    null)) {
                notices.staff(
                        "FINAL_PERIOD_OVERDUE",
                        state.finalStatementId(),
                        resident,
                        "M11.END_OVERDUE:" + resident + ":" + today,
                        "Đã quá ngày kết thúc dự kiến; cần lập phí các ngày đã kết thúc",
                        Role.OFFICE_STAFF,
                        slot,
                        null);
                count++;
            }
        }
        return count;
    }
}
