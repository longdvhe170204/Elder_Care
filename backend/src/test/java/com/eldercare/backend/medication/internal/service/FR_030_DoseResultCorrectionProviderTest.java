package com.eldercare.backend.medication.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.BillAdjustmentReason;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBillAdjustedEvent;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseResultType;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.account.api.spi.LicenseValidity;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.administration.correction.api.CorrectionView;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

class FR_030_DoseResultCorrectionProviderTest {

    private final DoseResultRepository results = mock(DoseResultRepository.class);
    private final MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
    private final ProfessionalActivityApi activities = mock(ProfessionalActivityApi.class);
    private final LicenseApi licenses = mock(LicenseApi.class);
    private final ScopeApi scope = mock(ScopeApi.class);
    private final NursingShiftPort shifts = mock(NursingShiftPort.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CorrectionApi corrections = mock(CorrectionApi.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CorrectionApi> correctionProvider = mock(ObjectProvider.class);

    private DoseResultCorrectionProvider provider;

    @BeforeEach
    void setUp() {
        when(time.today()).thenReturn(LocalDate.of(2026, 10, 29));
        when(time.now()).thenReturn(Instant.parse("2026-10-29T01:00:00Z"));
        when(correctionProvider.getObject()).thenReturn(corrections);
        provider = new DoseResultCorrectionProvider(
                results,
                doses,
                activities,
                licenses,
                scope,
                shifts,
                time,
                events,
                correctionProvider);
    }

    @Test
    void FR_030_onlyLicensedNurseCanCorrectDoseResult() {
        CurrentUser nurse = user(11L, 101L, Set.of(Role.NURSING_STAFF));
        when(activities.requiredLicenseType(ProfessionalActivityApi.MEDICATION_ADMINISTRATION))
                .thenReturn(Optional.of("NURSING_LICENSE"));

        assertThat(provider.canRecord(nurse)).isFalse();

        when(licenses.valid(101L, "NURSING_LICENSE", LocalDate.of(2026, 10, 29)))
                .thenReturn(Optional.of(
                        new LicenseValidity(1L, "Giấy phép hành nghề Điều dưỡng", LocalDate.of(2027, 1, 1), Set.of())));
        assertThat(provider.canRecord(nurse)).isTrue();
        assertThat(provider.canRecord(user(12L, 102L, Set.of(Role.FLOOR_MANAGER))))
                .isFalse();
    }

    @Test
    void FR_030_administeredCorrectionRequiresAdministeredAt() {
        assertThatThrownBy(() -> provider.validateContent(Map.of("resultType", "ADMINISTERED")))
                .isInstanceOf(InvalidValueException.class);

        provider.validateContent(Map.of("resultType", "ADMINISTERED", "administeredAt", "2026-10-29T00:30:00Z"));
        provider.validateContent(Map.of("resultType", "NOT_ADMINISTERED", "reason", "Người cao tuổi vắng mặt"));
    }

    @Test
    void FR_030_changingFacilityDoseAwayFromAdministeredPublishesZeroQuantityAdjustment() {
        DoseResult result = result(51L, 71L, DoseResultType.ADMINISTERED, null);
        MedicationDose dose = dose(71L, MedicationSource.FACILITY, new BigDecimal("1.50"));
        when(results.findById(51L)).thenReturn(Optional.of(result));
        when(doses.findById(71L)).thenReturn(Optional.of(dose));
        CorrectionView correction =
                correction(51L, Map.of("resultType", "NOT_ADMINISTERED", "reason", "Bác sĩ yêu cầu không dùng"));
        when(corrections.chain(DoseResultCorrectionProvider.RECORD_TYPE, 51L))
                .thenReturn(new CorrectionChainView(
                        DoseResultCorrectionProvider.RECORD_TYPE,
                        51L,
                        true,
                        false,
                        Map.of("resultType", "ADMINISTERED", "administeredAt", "2026-10-29T00:30:00Z"),
                        correction.correctedContent(),
                        List.of(correction)));

        provider.afterCorrection(correction);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue())
                .isEqualTo(new MedicationBillAdjustedEvent(
                        BilledSource.DOSE, 51L, BigDecimal.ZERO, BillAdjustmentReason.CORRECTION));
    }

    private CurrentUser user(long accountId, long staffId, Set<Role> roles) {
        CurrentUser user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(accountId);
        when(user.staffId()).thenReturn(Optional.of(staffId));
        when(user.roles()).thenReturn(roles);
        return user;
    }

    private DoseResult result(long id, long doseId, DoseResultType type, Long familyBatchId) {
        DoseResult result = mock(DoseResult.class);
        when(result.getId()).thenReturn(id);
        when(result.getDoseId()).thenReturn(doseId);
        when(result.getResultType()).thenReturn(type);
        when(result.getLeaveHandoverId()).thenReturn(null);
        return result;
    }

    private MedicationDose dose(long id, MedicationSource source, BigDecimal dosage) {
        MedicationDose dose = mock(MedicationDose.class);
        when(dose.getId()).thenReturn(id);
        when(dose.getMedicationSource()).thenReturn(source);
        when(dose.getDosage()).thenReturn(dosage);
        return dose;
    }

    private CorrectionView correction(long resultId, Map<String, Object> content) {
        return new CorrectionView(
                901L,
                DoseResultCorrectionProvider.RECORD_TYPE,
                resultId,
                1,
                CorrectionType.AMEND_CONTENT,
                content,
                "Ghi sai kết quả",
                77L,
                CreationMethod.A,
                null,
                Instant.parse("2026-10-29T01:00:00Z"));
    }
}
