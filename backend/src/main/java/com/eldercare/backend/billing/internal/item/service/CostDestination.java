package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.billing.internal.item.persistence.CostCreationStore;
import com.eldercare.backend.billing.internal.period.domain.CostStatementRecord;
import com.eldercare.backend.billing.internal.period.persistence.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-002a/025: quyết định bảng nhận khoản dưới khóa người của caller. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostDestination {
    private final CostPeriodStore periods;
    private final CostStatementStore statements;
    private final CostCreationStore creation;
    private final ResidentApi residents;
    private final BusinessTime time;
    private final com.eldercare.backend.administration.shared.api.ResidentModuleStateApi states;

    public CostDestination(
            CostPeriodStore periods,
            CostStatementStore statements,
            CostCreationStore creation,
            ResidentApi residents,
            BusinessTime time,
            com.eldercare.backend.administration.shared.api.ResidentModuleStateApi states) {
        this.periods = periods;
        this.statements = statements;
        this.creation = creation;
        this.residents = residents;
        this.time = time;
        this.states = states;
    }

    public CostStatementRecord regular(long resident, YearMonth month, long actor) {
        periods.ensurePeriod(month, time.now());
        long period = periods.periodId(month);
        statements.lockPeriod(period);
        long id = creation.statement(period, resident, "REGULAR", actor, time.now());
        return statements.lockedStatement(id);
    }

    public CostStatementRecord adjustment(long resident, long actor) {
        YearMonth month = YearMonth.from(time.today());
        periods.ensurePeriod(month, time.now());
        long period = periods.periodId(month);
        statements.lockPeriod(period);
        var regular = statements.list(period, resident, null).stream()
                .filter(s -> s.statementType().equals("REGULAR"))
                .findFirst();
        String type = residents.isFinalStatus(resident)
                        || finalizedFinalPeriod(resident).isPresent()
                        || regular.map(s -> s.status().equals("FINALIZED")).orElse(false)
                ? "SUPPLEMENTARY"
                : "REGULAR";
        long id = creation.statement(period, resident, type, actor, time.now());
        var target = statements.lockedStatement(id);
        if (target.status().equals("FINALIZED"))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "STATEMENT_FINALIZED", "Bảng nhận điều chỉnh của kỳ hiện tại đã chốt");
        return target;
    }

    public java.util.Optional<CostStatementRecord> finalizedFinalPeriod(long resident) {
        return states.get(com.eldercare.backend.billing.internal.period.domain.BillingResidentState.KIND, resident)
                .filter(s -> s.finalStatementId() != null)
                .map(s -> statements.statement(s.finalStatementId()))
                .filter(s -> s.status().equals("FINALIZED"));
    }
}
