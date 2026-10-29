package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.resident.api.ConsentApi;
import com.eldercare.backend.resident.api.events.ConsentChanged;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import com.eldercare.backend.resident.internal.consent.domain.ConsentStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T053 (US5 #4, #7; FR-014, FR-014a, FR-015, FR-016): Hành chính rút lại đồng ý kèm lý do và bản yêu cầu rút lại,
 * làm được cả trên hồ sơ trạng thái cuối; bản Đã rút lại không kích hoạt lại; người thân mất quyền ngay.
 */
class ConsentWithdrawalIT extends ConsentTestBase {

    private static final Reason REASON = new Reason("Người thân đại diện yêu cầu rút lại");

    @Autowired
    ConsentApi api;

    @Autowired
    ApplicationEvents events;

    private Consent withdraw(long residentId, long consentId, Reason reason, Long evidence) {
        user.set(office.user());
        return consents.withdraw(consents.targetOf(consentId), consentId, reason, evidence);
    }

    @Test
    void US5_withdraw_recordsReasonAndEvidence_andFamilyLosesScopeAtOnce() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        Consent consent = record(residentId, byResident(Set.of(x)));
        assertThat(api.includes(residentId, x)).isTrue();
        long evidence = evidence();

        Consent withdrawn = withdraw(residentId, consent.id(), REASON, evidence);

        assertThat(withdrawn.status()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(withdrawn.withdrawal().reason()).isEqualTo(REASON.value());
        assertThat(withdrawn.withdrawal().evidenceFileId()).isEqualTo(evidence);
        assertThat(withdrawn.withdrawal().supersededById()).isNull();
        assertThat(api.current(residentId)).isEmpty();
        assertThat(api.includes(residentId, x)).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT CONCAT(owner_type, ':', owner_id) FROM file WHERE id = ?", String.class, evidence))
                .isEqualTo("M01.CONSENT:" + consent.id());
        assertThat(auditActions(consent.id())).containsExactly("CONSENT_RECORDED", "CONSENT_WITHDRAWN");
        assertThat(events.stream(ConsentChanged.class)
                        .filter(e -> e.residentId() == residentId)
                        .map(ConsentChanged::activeConsentId)
                        .toList())
                .containsExactly(consent.id(), null);
    }

    @Test
    void FR_014_withoutReasonOrEvidence_isRejected_andConsentStaysActive() {
        long residentId = newResident();
        Consent consent = record(residentId, byResident(Set.of()));

        assertThatThrownBy(() -> withdraw(residentId, consent.id(), null, evidence()))
                .isInstanceOf(ReasonRequiredException.class);
        assertThatThrownBy(() -> withdraw(residentId, consent.id(), REASON, null))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("EVIDENCE_REQUIRED");
                    assertThat(e.status().value()).isEqualTo(422);
                });

        assertThat(activeCount(residentId)).isEqualTo(1);
    }

    @Test
    void FR_014a_withdrawalWorksOnClosedProfiles() {
        for (String status : new String[] {"RESIDENCY_ENDED", "DECEASED", "ADMISSION_CANCELLED"}) {
            long residentId = newResident();
            Consent consent = record(residentId, byResident(Set.of(relatedFamilyMember(residentId))));
            setStatus(residentId, status);

            Consent withdrawn = withdraw(residentId, consent.id(), REASON, evidence());

            assertThat(withdrawn.status()).as(status).isEqualTo(ConsentStatus.WITHDRAWN);
        }
    }

    @Test
    void FR_014_alreadyWithdrawn_is409_andCannotBeReactivated() throws Exception {
        long residentId = newResident();
        Consent consent = record(residentId, byResident(Set.of()));
        withdraw(residentId, consent.id(), REASON, evidence());

        assertThatThrownBy(() -> withdraw(residentId, consent.id(), REASON, evidence()))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("CONSENT_NOT_ACTIVE");
                    assertThat(e.status().value()).isEqualTo(409);
                });
        // trigger: chỉ chiều ACTIVE → WITHDRAWN (FR-014, FR-016)
        try (var admin = adminConnection();
                var reactivate = admin.prepareStatement("UPDATE consent SET status = 'ACTIVE' WHERE id = ?")) {
            reactivate.setLong(1, consent.id());
            assertThatThrownBy(reactivate::executeUpdate).hasMessageContaining("consent");
        }
    }

    @Test
    void FR_014_supersededConsentCannotBeWithdrawnAgain() {
        long residentId = newResident();
        Consent first = record(residentId, byResident(Set.of()));
        record(residentId, byResident(Set.of()));

        assertThatThrownBy(() -> withdraw(residentId, first.id(), REASON, evidence()))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CONSENT_NOT_ACTIVE"));
    }

    @Test
    void AC09_onlyOfficeStaffWithdraws() {
        long residentId = newResident();
        Consent consent = record(residentId, byResident(Set.of()));
        long evidence = evidence();

        user.set(doctor.user());
        assertThatThrownBy(() -> consents.withdraw(consents.targetOf(consent.id()), consent.id(), REASON, evidence))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(activeCount(residentId)).isEqualTo(1);
    }
}
