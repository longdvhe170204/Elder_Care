package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.PrnEffectivenessReminder;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.dose.domain.PrnEffectiveness;
import com.eldercare.backend.medication.internal.dose.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.dose.persistence.PrnEffectivenessRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.schedule.persistence.M07JobRunStore;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hiệu quả lần dùng thuốc khi cần và nhắc ghi sau CFG-M07-11 (BR-M07-04, Q-432). */
@Service
public class PrnEffectivenessService {

    private static final String JOB = "PRN_EFFECTIVENESS_REMINDER";
    // Giới hạn kỹ thuật của lần quét (không phải mốc nghiệp vụ): chỉ xét lần dùng trong 24 giờ gần nhất
    private static final int SCAN_BACK_HOURS = 24;

    private final PrnAdministrationRepository prn;
    private final PrnEffectivenessRepository effectiveness;
    private final PrescriptionRepository prescriptions;
    private final M07JobRunStore jobRuns;
    private final ParameterApi parameters;
    private final BusinessTime businessTime;
    private final ApplicationEventPublisher events;

    public PrnEffectivenessService(
            PrnAdministrationRepository prn,
            PrnEffectivenessRepository effectiveness,
            PrescriptionRepository prescriptions,
            M07JobRunStore jobRuns,
            ParameterApi parameters,
            BusinessTime businessTime,
            ApplicationEventPublisher events) {
        this.prn = prn;
        this.effectiveness = effectiveness;
        this.prescriptions = prescriptions;
        this.jobRuns = jobRuns;
        this.parameters = parameters;
        this.businessTime = businessTime;
        this.events = events;
    }

    @Transactional
    public PrnEffectiveness record(Long prescriptionId, Long prnId, String text, Long accountId) {
        if (text == null || text.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("EFFECTIVENESS_REQUIRED", "Hiệu quả sau khi dùng thuốc bắt buộc")));
        }
        PrnAdministration administration = prn.findById(prnId).orElseThrow(NotFoundException::new);
        if (!administration.getPrescriptionId().equals(prescriptionId)) {
            throw new NotFoundException();
        }
        if (effectiveness.findByDoseResultId(prnId).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "EFFECTIVENESS_ALREADY_RECORDED", "Lần dùng này đã ghi hiệu quả (BR-M07-04)");
        }
        return effectiveness.save(new PrnEffectiveness(prnId, text.trim(), accountId, businessTime.now()));
    }

    /** Hiệu quả đã ghi theo id lần dùng, để ghép vào lịch sử PRN. */
    @Transactional(readOnly = true)
    public Map<Long, PrnEffectiveness> byAdministration(Collection<Long> prnIds) {
        Map<Long, PrnEffectiveness> map = new HashMap<>();
        if (prnIds.isEmpty()) {
            return map;
        }
        for (PrnEffectiveness e : effectiveness.findByDoseResultIdIn(prnIds)) {
            map.put(e.getDoseResultId(), e);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public String indicationOf(Long prescriptionId) {
        return prescriptions
                .findById(prescriptionId)
                .map(Prescription::getIndication)
                .orElse(null);
    }

    /** Việc theo phút (BR-M07-04): nhắc một lần mỗi lần dùng đã quá CFG-M07-11 mà chưa ghi hiệu quả. */
    @Transactional
    public void remind(Instant now) {
        Instant cutoff = now.minus(parameters.read(ParameterCode.CFG_M07_11).asDuration());
        for (PrnAdministration a : prn.findAwaitingEffectiveness(
                cutoff, now.minus(SCAN_BACK_HOURS, java.time.temporal.ChronoUnit.HOURS))) {
            String checkpoint = String.valueOf(a.getId());
            if (jobRuns.isCompleted(JOB, checkpoint)) {
                continue;
            }
            events.publishEvent(new PrnEffectivenessReminder(a.getId(), a.getResidentId(), now));
            jobRuns.recordCompletion(JOB, checkpoint, "COMPLETED");
        }
    }
}
