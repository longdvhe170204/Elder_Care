package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.administration.correction.api.CorrectionView;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.BillAdjustmentReason;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBillAdjustedEvent;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Provider đính chính kết quả liều thuốc (FR-030, research D-12). */
@Component
class DoseResultCorrectionProvider implements CorrectableRecordProvider {

    static final String RECORD_TYPE = DoseResultOfflineHandler.RECORD_TYPE;
    private static final Set<String> RESULT_TYPES = Set.of("ADMINISTERED", "REFUSED", "NOT_ADMINISTERED", "MISSED");

    private final DoseResultRepository results;
    private final MedicationDoseRepository doses;
    private final ProfessionalActivityApi activities;
    private final LicenseApi licenses;
    private final ScopeApi scope;
    private final NursingShiftPort shifts;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<CorrectionApi> corrections;

    DoseResultCorrectionProvider(
            DoseResultRepository results,
            MedicationDoseRepository doses,
            ProfessionalActivityApi activities,
            LicenseApi licenses,
            ScopeApi scope,
            NursingShiftPort shifts,
            BusinessTime time,
            ApplicationEventPublisher events,
            ObjectProvider<CorrectionApi> corrections) {
        this.results = results;
        this.doses = doses;
        this.activities = activities;
        this.licenses = licenses;
        this.scope = scope;
        this.shifts = shifts;
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
        return results.existsById(id);
    }

    @Override
    public long originalRecorder(long id) {
        DoseResult result = result(id);
        if (result.getRecorderId() != null) {
            return result.getRecorderId();
        }
        MedicationDose dose = dose(result);
        return shifts.assignedTo(result.getResidentId(), dose.getScheduledTime())
                .orElse(0L);
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(result(id).getFloorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(result(id).getResidentId());
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
        if (!(correctedContent instanceof Map<?, ?> content)
                || !(content.get("resultType") instanceof String resultType)
                || !RESULT_TYPES.contains(resultType)) {
            throw new InvalidValueException(
                    "Nội dung đúng cần resultType (ADMINISTERED, REFUSED, NOT_ADMINISTERED, MISSED)");
        }
        if ("ADMINISTERED".equals(resultType)) {
            parseAdministeredAt(content.get("administeredAt"));
        }
        if (("REFUSED".equals(resultType) || "NOT_ADMINISTERED".equals(resultType))
                && !(content.get("reason") instanceof String reason && !reason.isBlank())) {
            throw new InvalidValueException("Từ chối hoặc Không thực hiện cần lý do");
        }
    }

    @Override
    public Object originalContent(long id) {
        DoseResult result = result(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("resultType", result.getResultType().name());
        put(content, "administeredAt", result.getAdministeredAt());
        put(content, "adverseReaction", result.getAdverseReaction());
        put(content, "reason", result.getReason());
        put(content, "note", result.getNote());
        return content;
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        DoseResult result = result(correction.originalRecordId());
        MedicationDose dose = dose(result);
        CorrectionChainView chain = corrections.getObject().chain(RECORD_TYPE, result.getId());
        Outcome before = outcome(contentBefore(chain, correction));
        Outcome after =
                correction.type() == CorrectionType.VOID ? Outcome.empty() : outcome(correction.correctedContent());

        if (dose.getMedicationSource() == MedicationSource.FACILITY
                && result.getLeaveHandoverId() == null
                && before.administered() != after.administered()) {
            events.publishEvent(new MedicationBillAdjustedEvent(
                    BilledSource.DOSE,
                    result.getId(),
                    after.administered() ? dose.getDosage() : BigDecimal.ZERO,
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
        if (!(value instanceof Map<?, ?> content) || !(content.get("resultType") instanceof String resultType)) {
            return Outcome.empty();
        }
        return new Outcome("ADMINISTERED".equals(resultType));
    }

    private DoseResult result(long id) {
        return results.findById(id).orElseThrow(com.eldercare.backend.common.error.NotFoundException::new);
    }

    private MedicationDose dose(DoseResult result) {
        return doses.findById(result.getDoseId())
                .orElseThrow(com.eldercare.backend.common.error.NotFoundException::new);
    }

    private static Instant parseAdministeredAt(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text) {
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
                // Báo cùng lỗi nội dung ở dưới.
            }
        }
        throw new InvalidValueException("Đã dùng cần administeredAt theo ISO-8601");
    }

    private static void put(Map<String, Object> content, String key, Object value) {
        if (value != null) {
            content.put(key, value);
        }
    }

    private record Outcome(boolean administered) {
        static Outcome empty() {
            return new Outcome(false);
        }
    }
}
