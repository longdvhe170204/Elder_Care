package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.admission.api.spi.FinalPeriodPort;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
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
}
