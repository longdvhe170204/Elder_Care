package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.admission.api.spi.FinalPeriodPort;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.StatusTestBase;
import com.eldercare.backend.resident.api.dto.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 001 và 010 thật; các participant còn lại dùng fixture chung do chủ module khai báo. */
class FinalPeriodStatusIT extends StatusTestBase {
    @Autowired
    FinalPeriodPort finalPeriods;

    @Autowired
    CostStatementStore statements;

    @Autowired
    ResidentModuleStateApi states;

    @Test
    void KT_A_CHI_PHI_previewAndCommandShareTheActualBillingCondition() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        inTransaction(() -> finalPeriods.createDraft(resident, time.today()));
        assertThat(statusService.previewConditions(target(resident), StatusCommand.END_RESIDENCY))
                .extracting(Condition::code)
                .contains("END_A_COSTS");
        assertThatThrownBy(() -> statusService.endResidency(target(resident), reason(), noDetails()))
                .isInstanceOf(BusinessException.class);
        assertThat(currentStatus(resident)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_034a_realResidentCanEndAfterFinalizedPeriod() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        long statement = inTransaction(() -> finalPeriods.createDraft(resident, time.today()));
        inTransaction(() -> {
            statements.finalizeStatement(statement, office.accountId(), time.now(), Map.of());
            return null;
        });
        assertThat(statusService
                        .endResidency(target(resident), reason(), noDetails())
                        .status())
                .isEqualTo(ResidentStatus.RESIDENCY_ENDED);
    }

    @Test
    void KT_A_CHI_PHI_approvedWaiverUsesTheSharedEnglishConditionCode() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        inTransaction(() -> finalPeriods.createDraft(resident, time.today()));
        two.waive(new ConditionWaiver(501L, java.util.Set.of("END_A_COSTS")));
        assertThat(statusService
                        .endResidency(target(resident), reason(), noDetails())
                        .status())
                .isEqualTo(ResidentStatus.RESIDENCY_ENDED);
    }

    @Test
    void FR_047_deathWithoutAdmissionDetailsUsesThePublishedTransitionTime() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(users.staff(Role.DOCTOR).user());
        var outcome = statusService.recordDeath(target(resident), reason(), noDetails());
        assertThat(outcome.status()).isEqualTo(ResidentStatus.DECEASED);
        var state = states.get(BillingResidentState.KIND, resident).orElseThrow();
        assertThat(state.deathAt()).isEqualTo(outcome.history().at());
        assertThat(state.throughDate())
                .isEqualTo(java.time.LocalDate.ofInstant(
                        outcome.history().at(), com.eldercare.backend.common.time.BusinessTime.ZONE));
    }

    @Test
    void FR_047_failureAfterBillingDeathEffectsRollsBackResidentStatementAndState() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(users.staff(Role.DOCTOR).user());
        two.failOnApply(new IllegalStateException("Module sau 010 lỗi"));
        var details = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        details.put("deathAt", time.now().toString());
        assertThatThrownBy(() -> statusService.recordDeath(target(resident), reason(), details))
                .isInstanceOf(IllegalStateException.class);
        assertThat(currentStatus(resident)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(states.get(BillingResidentState.KIND, resident)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Long.class, resident))
                .isZero();
    }
}
