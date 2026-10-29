package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.MedicationStatus;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
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
                    "DUPLICATE_MEDICATION",
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
                    "DUPLICATE_MEDICATION",
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
                    List.of(new UnmetCondition("NAME_REQUIRED", "Medication name is required (FR-001)")));
        }
        if (concentration == null || concentration.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("CONCENTRATION_REQUIRED", "Concentration is required (FR-001)")));
        }
        if (dosageForm == null || dosageForm.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("DOSAGE_FORM_REQUIRED", "Dosage form is required (FR-001)")));
        }
        if (defaultRoute == null || defaultRoute.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("ROUTE_REQUIRED", "Default route is required (FR-001)")));
        }
        if (usageUnit == null || usageUnit.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("UNIT_REQUIRED", "Usage unit is required (FR-001)")));
        }
        if (allergenIds == null || allergenIds.isEmpty()) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "ACTIVE_INGREDIENT_REQUIRED",
                    "Medication must be linked to at least one active ingredient / allergen (FR-001)")));
        }
        if (beforeWindow != null && beforeWindow < 0) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("INVALID_WINDOW", "Before window must be non-negative")));
        }
        if (afterWindow != null && afterWindow < 0) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("INVALID_WINDOW", "After window must be non-negative")));
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
