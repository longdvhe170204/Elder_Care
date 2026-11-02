package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.ConsentApi;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import com.eldercare.backend.resident.internal.consent.domain.CourtDecision;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T053 (US5 #3, #10; FR-011, Q-348, Q-355): người ký thay người cao tuổi chỉ là người đại diện theo pháp luật, kèm
 * đủ bốn thông tin quyết định của Tòa án; vai trò "người đại diện" của 012 không cho quyền ký thay; người ký phải có
 * quan hệ Hiệu lực với người cao tuổi.
 */
class FR_011_LegalRepresentativeIT extends ConsentTestBase {

    @Autowired
    ConsentApi api;

    private CourtDecision decision() {
        return new CourtDecision("12/2026/QĐ-TA", LocalDate.of(2026, 3, 5), "Tòa án nhân dân quận 1", evidence());
    }

    private RecordConsent byRepresentative(Long signer, CourtDecision decision, Set<Long> allowed) {
        return new RecordConsent(
                ConsenterType.LEGAL_REPRESENTATIVE, signer, decision, allowed, true, true, Instant.now(), evidence());
    }

    private void assertCourtDecisionRequired(long residentId, RecordConsent command) {
        assertThatThrownBy(() -> record(residentId, command)).isInstanceOfSatisfying(BusinessException.class, e -> {
            assertThat(e.errorCode()).isEqualTo("COURT_DECISION_REQUIRED");
            assertThat(e.status().value()).isEqualTo(422);
        });
        assertThat(activeCount(residentId)).isZero();
    }

    @Test
    void US5_legalRepresentativeWithFullDecision_isRecorded_andExposedToOtherModules() {
        long residentId = newResident();
        long signer = relatedFamilyMember(residentId);
        CourtDecision decision = decision();

        Consent saved = record(residentId, byRepresentative(signer, decision, Set.of(signer)));

        assertThat(saved.consenterType()).isEqualTo(ConsenterType.LEGAL_REPRESENTATIVE);
        assertThat(saved.consenterFamilyMemberId()).isEqualTo(signer);
        assertThat(saved.courtDecision()).isEqualTo(decision);
        assertThat(api.legalRepresentative(residentId)).contains(signer);
        // tệp quyết định của Tòa án gắn cùng bản đồng ý
        assertThat(jdbc.queryForObject(
                        "SELECT CONCAT(owner_type, ':', owner_id) FROM file WHERE id = ?",
                        String.class,
                        decision.fileId()))
                .isEqualTo("M01.CONSENT:" + saved.id());
    }

    @Test
    void FR_011_withoutCourtDecision_is422_evenForTheRepresentativeOf012() {
        long residentId = newResident();
        long signer = relatedFamilyMember(residentId);
        families.representative(families.relate(signer, residentId));

        assertCourtDecisionRequired(residentId, byRepresentative(signer, null, Set.of()));
    }

    @Test
    void FR_011_eachOfTheFourDecisionFieldsIsRequired() {
        long residentId = newResident();
        long signer = relatedFamilyMember(residentId);
        LocalDate date = LocalDate.of(2026, 3, 5);

        assertCourtDecisionRequired(
                residentId, byRepresentative(signer, new CourtDecision(" ", date, "Tòa án", evidence()), Set.of()));
        assertCourtDecisionRequired(
                residentId, byRepresentative(signer, new CourtDecision("12/QĐ", null, "Tòa án", evidence()), Set.of()));
        assertCourtDecisionRequired(
                residentId, byRepresentative(signer, new CourtDecision("12/QĐ", date, "", evidence()), Set.of()));
        assertCourtDecisionRequired(
                residentId, byRepresentative(signer, new CourtDecision("12/QĐ", date, "Tòa án", null), Set.of()));
    }

    @Test
    void FR_011_signerWithoutActiveRelationship_is409() {
        long residentId = newResident();
        long stranger = newFamilyMember();

        assertThatThrownBy(() -> record(residentId, byRepresentative(stranger, decision(), Set.of())))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("INVALID_SIGNER");
                    assertThat(e.status().value()).isEqualTo(409);
                });
        assertThat(activeCount(residentId)).isZero();
    }

    @Test
    void FR_011_signerIdIsRequiredForRepresentative_andNotAllowedForTheResident() {
        long residentId = newResident();

        assertThatThrownBy(() -> record(residentId, byRepresentative(null, decision(), Set.of())))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("INVALID_SIGNER");
                });
        long signer = relatedFamilyMember(residentId);
        assertThatThrownBy(() -> record(
                        residentId,
                        new RecordConsent(
                                ConsenterType.RESIDENT, signer, null, Set.of(), true, true, Instant.now(), evidence())))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("INVALID_SIGNER");
                });
        assertThatThrownBy(() -> record(
                        residentId,
                        new RecordConsent(
                                ConsenterType.RESIDENT,
                                null,
                                decision(),
                                Set.of(),
                                true,
                                true,
                                Instant.now(),
                                evidence())))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("COURT_DECISION_NOT_ALLOWED");
                });
    }

    @Test
    void Q355_residentWhoCannotSign_hasNoWayToRecord_soNoFamilyMemberSeesHealth() {
        long residentId = newResident();
        long child = relatedFamilyMember(residentId);

        // người con ký thay mà chưa có quyết định của Tòa án (US5 #10)
        assertCourtDecisionRequired(residentId, byRepresentative(child, null, Set.of(child)));
        assertThat(api.current(residentId)).isEmpty();
        assertThat(api.includes(residentId, child)).isFalse();
    }
}
