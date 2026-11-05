package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/** Dòng thuốc giao nhúng trong {@code medication_handover.lines.items} (006 T069, {@code V006_97}). */
@Repository
public class MedicationHandoverItemRepository {

    private final MedicationHandoverRepository handovers;

    public MedicationHandoverItemRepository(MedicationHandoverRepository handovers) {
        this.handovers = handovers;
    }

    public List<MedicationHandoverItem> findByHandoverId(Long handoverId) {
        return handovers.findById(handoverId).map(MedicationHandover::getItems).orElseGet(List::of);
    }

    /** Thêm dòng vào lần giao; id dòng tăng dần trong lần giao, giữ id cũ của dòng đã chép. */
    public MedicationHandoverItem save(MedicationHandoverItem item) {
        MedicationHandover handover = handovers
                .findById(item.getHandoverId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lần giao thuốc"));
        List<MedicationHandoverItem> items = new ArrayList<>(handover.getItems());
        if (item.getId() == null) {
            item.assignId(items.stream()
                            .map(MedicationHandoverItem::getId)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .max()
                            .orElse(0L)
                    + 1);
        }
        items.removeIf(existing -> Objects.equals(existing.getId(), item.getId()));
        items.add(item);
        handover.replaceLines(items, handover.getDoses());
        handovers.save(handover);
        return item;
    }
}
