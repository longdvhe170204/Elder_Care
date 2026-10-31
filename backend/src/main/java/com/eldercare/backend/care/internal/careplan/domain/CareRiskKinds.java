package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** Hai loại con do M04 sở hữu; không công bố cho module khác đọc trực tiếp. */
public final class CareRiskKinds {
    private CareRiskKinds() {}

    public static final SharedKind<RiskParticipant> PARTICIPANTS =
            SharedKind.of("M04.RISK_PARTICIPANT", RiskParticipant.class);
    public static final SharedKind<RiskOpinion> OPINIONS = SharedKind.of("M04.RISK_OPINION", RiskOpinion.class);
}
