package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationStatus;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business service for Medication Catalog (FR-001, FR-002, FR-003, AC-01; research D-04).
 */
@Service
public class MedicationService {

    private final MedicationRepository medications;
    private final MedicationCatalogPort catalogPort;

    public MedicationService(MedicationRepository medications, MedicationCatalogPort catalogPort) {
        this.medications = medications;
        this.catalogPort = catalogPort;
    }

    @RequiresPermission(function = Function.MEDICATION_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<MedicationDetailView> list(String q, Boolean activeOnly) {
        MedicationStatus status = (activeOnly != null && activeOnly) ? MedicationStatus.ACTIVE : null;
        return medications.search(q, status).stream().map(this::toView).toList();
    }

    @RequiresPermission(function = Function.MEDICATION_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public MedicationDetailView getById(long id) {
        Medication med = medications.findById(id).orElseThrow(NotFoundException::new);
        return toView(med);
    }

    @RequiresPermission(function = Function.MEDICATION_CATALOG, operation = Operation.CREATE)
    @Transactional
    public MedicationDetailView create(CreateMedicationCommand cmd) {
        validateCommand(
                cmd.name(),
                cmd.concentration(),
                cmd.dosageForm(),
                cmd.defaultRoute(),
                cmd.usageUnit(),
                cmd.allergenIds(),
                cmd.beforeWindowMinutes(),
                cmd.afterWindowMinutes());

        if (medications.existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCase(
                cmd.name().trim(), cmd.concentration().trim(), cmd.dosageForm().trim())) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "TRUNG_THUOC",
                    "Medication already exists with the same name, concentration, and dosage form (FR-001)")));
        }

        Medication med = new Medication(
                cmd.name().trim(),
                cmd.concentration().trim(),
                cmd.dosageForm().trim(),
                cmd.defaultRoute().trim(),
                cmd.usageUnit().trim(),
                cmd.beforeWindowMinutes(),
                cmd.afterWindowMinutes(),
                cmd.specialControl(),
                cmd.allergenIds());

        Medication saved = medications.save(med);
        return toView(saved);
    }

    @RequiresPermission(function = Function.MEDICATION_CATALOG, operation = Operation.UPDATE)
    @Transactional
    public MedicationDetailView update(long id, String ifMatch, UpdateMedicationCommand cmd) {
        Medication med = medications.findById(id).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, med.getVersion(), toView(med));

        validateCommand(
                cmd.name(),
                cmd.concentration(),
                cmd.dosageForm(),
                cmd.defaultRoute(),
                cmd.usageUnit(),
                cmd.allergenIds(),
                cmd.beforeWindowMinutes(),
                cmd.afterWindowMinutes());

        if (medications.existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCaseAndIdNot(
                cmd.name().trim(), cmd.concentration().trim(), cmd.dosageForm().trim(), id)) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "TRUNG_THUOC",
                    "Medication already exists with the same name, concentration, and dosage form (FR-001)")));
        }

        med.update(
                cmd.name().trim(),
                cmd.concentration().trim(),
                cmd.dosageForm().trim(),
                cmd.defaultRoute().trim(),
                cmd.usageUnit().trim(),
                cmd.beforeWindowMinutes(),
                cmd.afterWindowMinutes(),
                cmd.specialControl(),
                cmd.allergenIds());

        return toView(med);
    }

    @RequiresPermission(function = Function.MEDICATION_CATALOG, operation = Operation.UPDATE)
    @Transactional
    public MedicationDetailView deactivate(long id, Reason reason) {
        Medication med = medications.findById(id).orElseThrow(NotFoundException::new);
        med.deactivate();
        return toView(med);
    }

    private void validateCommand(
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            Set<Long> allergenIds,
            Integer beforeWindow,
            Integer afterWindow) {
        if (name == null || name.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THIEU_TEN", "Medication name is required (FR-001)")));
        }
        if (concentration == null || concentration.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THIEU_HAM_LUONG", "Concentration is required (FR-001)")));
        }
        if (dosageForm == null || dosageForm.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THIEU_DANG_BAO_CHE", "Dosage form is required (FR-001)")));
        }
        if (defaultRoute == null || defaultRoute.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THIEU_DUONG_DUNG", "Default route is required (FR-001)")));
        }
        if (usageUnit == null || usageUnit.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THIEU_DON_VI", "Usage unit is required (FR-001)")));
        }
        if (allergenIds == null || allergenIds.isEmpty()) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "THIEU_HOAT_CHAT",
                    "Medication must be linked to at least one active ingredient / allergen (FR-001)")));
        }
        if (beforeWindow != null && beforeWindow < 0) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("CUA_SO_KHONG_HOP_LE", "Before window must be non-negative")));
        }
        if (afterWindow != null && afterWindow < 0) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("CUA_SO_KHONG_HOP_LE", "After window must be non-negative")));
        }
    }

    private MedicationDetailView toView(Medication m) {
        boolean missingPricing = !catalogPort.hasHospitalProcurementItem(m.getId());
        return new MedicationDetailView(
                m.getId(),
                m.getName(),
                m.getConcentration(),
                m.getDosageForm(),
                m.getDefaultRoute(),
                m.getUsageUnit(),
                List.copyOf(m.getAllergenIds()),
                m.getBeforeWindowMinutes(),
                m.getAfterWindowMinutes(),
                m.isSpecialControl(),
                m.isActive(),
                missingPricing,
                m.getVersion());
    }

    public record CreateMedicationCommand(
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            Set<Long> allergenIds,
            Integer beforeWindowMinutes,
            Integer afterWindowMinutes,
            boolean specialControl) {}

    public record UpdateMedicationCommand(
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            Set<Long> allergenIds,
            Integer beforeWindowMinutes,
            Integer afterWindowMinutes,
            boolean specialControl) {}

    public record MedicationDetailView(
            Long id,
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            List<Long> allergenIds,
            Integer beforeWindowMinutes,
            Integer afterWindowMinutes,
            boolean specialControl,
            boolean active,
            boolean missingPricingItem,
            int version) {}
}
