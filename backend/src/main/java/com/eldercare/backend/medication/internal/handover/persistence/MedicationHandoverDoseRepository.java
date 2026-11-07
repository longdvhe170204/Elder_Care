package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverDose;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Liều mang theo nhúng trong {@code medication_handover.lines.doses} (006 T069, {@code V006_97}). Một liều thuộc tối đa
 * một lần giao: thay {@code UNIQUE (dose_id)} của bảng cũ, kiểm khi thêm.
 */
@Repository
public class MedicationHandoverDoseRepository {

    private final MedicationHandoverRepository handovers;

    public MedicationHandoverDoseRepository(MedicationHandoverRepository handovers) {
        this.handovers = handovers;
    }

    public List<MedicationHandoverDose> findByHandoverId(Long handoverId) {
        return handovers.findById(handoverId).map(MedicationHandover::getDoses).orElseGet(List::of);
    }

    public Optional<MedicationHandoverDose> findByDoseId(Long doseId) {
        return handovers
                .findByDoseId(doseId)
                .flatMap(handover -> handover.getDoses().stream()
                        .filter(link -> Objects.equals(link.getDoseId(), doseId))
                        .findFirst());
    }

    public MedicationHandoverDose save(MedicationHandoverDose link) {
        Optional<MedicationHandoverDose> existing = findByDoseId(link.getDoseId());
        if (existing.isPresent() && !Objects.equals(existing.get().getHandoverId(), link.getHandoverId())) {
            throw new IllegalStateException("Liều " + link.getDoseId() + " đã thuộc lần giao khác");
        }
        MedicationHandover handover = handovers
                .findById(link.getHandoverId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lần giao thuốc"));
        List<MedicationHandoverDose> doses = new ArrayList<>(handover.getDoses());
        if (link.getId() == null) {
            link.assignId(doses.stream()
                            .map(MedicationHandoverDose::getId)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .max()
                            .orElse(0L)
                    + 1);
        }
        doses.removeIf(d -> Objects.equals(d.getDoseId(), link.getDoseId()));
        doses.add(link);
        handover.replaceLines(handover.getItems(), doses);
        handovers.save(handover);
        return link;
    }
}
