package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
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
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Provider đính chính lần dùng thuốc PRN (FR-030, research D-12). */
@Component
class PrnCorrectionProvider implements CorrectableRecordProvider {

    static final String RECORD_TYPE = PrnOfflineHandler.RECORD_TYPE;

    private final PrnAdministrationRepository administrations;
    private final PrescriptionRepository prescriptions;
    private final ProfessionalActivityApi activities;
    private final LicenseApi licenses;
    private final ScopeApi scope;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<CorrectionApi> corrections;

    PrnCorrectionProvider(
            PrnAdministrationRepository administrations,
            PrescriptionRepository prescriptions,
            ProfessionalActivityApi activities,
            LicenseApi licenses,
            ScopeApi scope,
            BusinessTime time,
            ApplicationEventPublisher events,
            ObjectProvider<CorrectionApi> corrections) {
        this.administrations = administrations;
        this.prescriptions = prescriptions;
        this.activities = activities;
        this.licenses = licenses;
        this.scope = scope;
        this.time = time;
        this.events = events;
        this.corrections = corrections;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return administrations.existsById(id);
    }

    @Override
    public long originalRecorder(long id) {
        return administration(id).getAdministeredById();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(administration(id).getFloorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(administration(id).getResidentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        if (!user.roles().contains(Role.NURSING_STAFF) || user.staffId().isEmpty()) {
            return false;
        }
        Optional<String> required = activities.requiredLicenseType(ProfessionalActivityApi.MEDICATION_ADMINISTRATION);
        return required.isPresent()
                && licenses.valid(user.staffId().get(), required.get(), time.today())
                        .isPresent();
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        if (!canRecord(user) || method == CreationMethod.C) {
            return false;
        }
        if (method == CreationMethod.A) {
            return originalRecorder(id) == user.accountId();
        }
        return floorArea(id)
                .flatMap(floor -> scope.shiftLead(floor, time.now()))
                .filter(lead -> lead == user.accountId())
                .isPresent();
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> content)) {
            throw new InvalidValueException("Nội dung đúng của lần dùng PRN phải là một đối tượng");
        }
        parseInstant(content.get("administeredAt"));
        if (decimal(content.get("dosage")).signum() <= 0) {
            throw new InvalidValueException("Liều dùng PRN phải lớn hơn 0");
        }
        if (!(content.get("reason") instanceof String reason) || reason.isBlank()) {
            throw new InvalidValueException("Lần dùng PRN cần lý do");
        }
    }

    @Override
    public Object originalContent(long id) {
        PrnAdministration administration = administration(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("administeredAt", administration.getAdministeredAt().toString());
        content.put("dosage", administration.getDosage());
        content.put("reason", administration.getReason());
        put(content, "adverseReaction", administration.getAdverseReaction());
        put(content, "note", administration.getNote());
        return content;
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        PrnAdministration administration = administration(correction.originalRecordId());
        Prescription prescription = prescription(administration.getPrescriptionId());
        CorrectionChainView chain = corrections.getObject().chain(RECORD_TYPE, administration.getId());
        Outcome before = outcome(contentBefore(chain, correction));
        Outcome after =
                correction.type() == CorrectionType.VOID ? Outcome.empty() : outcome(correction.correctedContent());

        if (prescription.getMedicationSource() == MedicationSource.FACILITY
                && before.quantity().compareTo(after.quantity()) != 0) {
            events.publishEvent(new MedicationBillAdjustedEvent(
                    BilledSource.PRN_ADMINISTRATION,
                    administration.getId(),
                    after.quantity(),
                    BillAdjustmentReason.CORRECTION));
        }
    }

    private Object contentBefore(CorrectionChainView chain, CorrectionView current) {
        Object content = chain.originalContent();
        for (CorrectionView item : chain.corrections()) {
            if (item.sequenceNo() >= current.sequenceNo()) {
                break;
            }
            content = item.type() == CorrectionType.VOID ? null : item.correctedContent();
        }
        return content;
    }

    private Outcome outcome(Object value) {
        if (!(value instanceof Map<?, ?> content)) {
            return Outcome.empty();
        }
        BigDecimal quantity = decimal(content.get("dosage"));
        return new Outcome(true, quantity);
    }

    private PrnAdministration administration(long id) {
        return administrations.findById(id).orElseThrow(com.eldercare.backend.common.error.NotFoundException::new);
    }

    private Prescription prescription(long id) {
        return prescriptions.findById(id).orElseThrow(com.eldercare.backend.common.error.NotFoundException::new);
    }

    private static Instant parseInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text) {
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
                // Báo lỗi nội dung thống nhất ở dưới.
            }
        }
        throw new InvalidValueException("Lần dùng PRN cần administeredAt theo ISO-8601");
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                // Báo lỗi nội dung thống nhất ở dưới.
            }
        }
        throw new InvalidValueException("Lần dùng PRN cần dosage là số");
    }

    private static void put(Map<String, Object> content, String key, Object value) {
        if (value != null) {
            content.put(key, value);
        }
    }

    private record Outcome(boolean active, BigDecimal quantity) {
        static Outcome empty() {
            return new Outcome(false, BigDecimal.ZERO);
        }
    }
}
