package com.eldercare.backend.medication.internal.dose.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.administration.correction.api.CorrectionView;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.BillAdjustmentReason;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBillAdjustedEvent;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.dose.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
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

class FR_030_PrnCorrectionProviderTest {

    private final PrnAdministrationRepository administrations = mock(PrnAdministrationRepository.class);
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final ProfessionalActivityApi activities = mock(ProfessionalActivityApi.class);
    private final LicenseApi licenses = mock(LicenseApi.class);
    private final ScopeApi scope = mock(ScopeApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CorrectionApi corrections = mock(CorrectionApi.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CorrectionApi> correctionProvider = mock(ObjectProvider.class);

    private PrnCorrectionProvider provider;

    @BeforeEach
    void setUp() {
        when(time.today()).thenReturn(LocalDate.of(2026, 10, 29));
        when(time.now()).thenReturn(Instant.parse("2026-10-29T03:00:00Z"));
        when(correctionProvider.getObject()).thenReturn(corrections);
        provider = new PrnCorrectionProvider(
                administrations, prescriptions, activities, licenses, scope, time, events, correctionProvider);
    }

    @Test
    void FR_030_prnProviderExposesOriginalRecordContext() {
        PrnAdministration administration = administration(81L, 31L, 41L, 9L, 71L, new BigDecimal("2.00"), 91L);
        when(administrations.findById(81L)).thenReturn(Optional.of(administration));
        when(administrations.existsById(81L)).thenReturn(true);

        assertThat(provider.recordType()).isEqualTo("M07.PRN_ADMINISTRATION");
        assertThat(provider.isConfirmed(81L)).isTrue();
        assertThat(provider.originalRecorder(81L)).isEqualTo(71L);
        assertThat(provider.floorArea(81L)).contains(9L);
        assertThat(provider.residentId(81L)).contains(41L);
        assertThat(provider.originalContent(81L))
                .isEqualTo(Map.of(
                        "administeredAt",
                        "2026-10-29T02:00:00Z",
                        "dosage",
                        new BigDecimal("2.00"),
                        "reason",
                        "Đau nhiều"));
    }

    @Test
    void FR_030_prnCorrectionRequiresPositiveDosageAndReason() {
        assertThatThrownBy(() -> provider.validateContent(
                        Map.of("administeredAt", "2026-10-29T02:00:00Z", "dosage", BigDecimal.ZERO, "reason", "Đau")))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> provider.validateContent(
                        Map.of("administeredAt", "2026-10-29T02:00:00Z", "dosage", new BigDecimal("1.00"))))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_030_facilityPrnCorrectionPublishesNewBillQuantity() {
        PrnAdministration administration = administration(81L, 31L, 41L, 9L, 71L, new BigDecimal("2.00"), null);
        Prescription prescription = prescription(MedicationSource.FACILITY);
        when(administrations.findById(81L)).thenReturn(Optional.of(administration));
        when(prescriptions.findById(31L)).thenReturn(Optional.of(prescription));
        CorrectionView correction = correction(Map.of(
                "administeredAt", "2026-10-29T02:00:00Z",
                "dosage", new BigDecimal("1.25"),
                "reason", "Đau vừa"));
        chain(
                correction,
                Map.of(
                        "administeredAt", "2026-10-29T02:00:00Z",
                        "dosage", new BigDecimal("2.00"),
                        "reason", "Đau nhiều"));

        provider.afterCorrection(correction);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue())
                .isEqualTo(new MedicationBillAdjustedEvent(
                        BilledSource.PRN_ADMINISTRATION, 81L, new BigDecimal("1.25"), BillAdjustmentReason.CORRECTION));
    }

    private void chain(CorrectionView correction, Map<String, Object> original) {
        when(corrections.chain(PrnCorrectionProvider.RECORD_TYPE, 81L))
                .thenReturn(new CorrectionChainView(
                        PrnCorrectionProvider.RECORD_TYPE,
                        81L,
                        true,
                        false,
                        original,
                        correction.correctedContent(),
                        List.of(correction)));
    }

    private CorrectionView correction(Map<String, Object> content) {
        return new CorrectionView(
                901L,
                "M07.PRN_ADMINISTRATION",
                81L,
                1,
                CorrectionType.AMEND_CONTENT,
                content,
                "Ghi sai lần dùng PRN",
                77L,
                CreationMethod.A,
                null,
                Instant.parse("2026-10-29T03:00:00Z"));
    }

    private PrnAdministration administration(
            long id,
            long prescriptionId,
            long residentId,
            long floorId,
            long recorderId,
            BigDecimal dosage,
            Long batchId) {
        PrnAdministration administration = mock(PrnAdministration.class);
        when(administration.getId()).thenReturn(id);
        when(administration.getPrescriptionId()).thenReturn(prescriptionId);
        when(administration.getResidentId()).thenReturn(residentId);
        when(administration.getFloorAreaId()).thenReturn(floorId);
        when(administration.getAdministeredById()).thenReturn(recorderId);
        when(administration.getAdministeredAt()).thenReturn(Instant.parse("2026-10-29T02:00:00Z"));
        when(administration.getDosage()).thenReturn(dosage);
        when(administration.getReason()).thenReturn("Đau nhiều");
        return administration;
    }

    private Prescription prescription(MedicationSource source) {
        Prescription prescription = mock(Prescription.class);
        when(prescription.getMedicationSource()).thenReturn(source);
        return prescription;
    }

    @SuppressWarnings("unused")
    private CurrentUser user(long accountId, long staffId, Set<Role> roles) {
        CurrentUser user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(accountId);
        when(user.staffId()).thenReturn(Optional.of(staffId));
        when(user.roles()).thenReturn(roles);
        return user;
    }
}
