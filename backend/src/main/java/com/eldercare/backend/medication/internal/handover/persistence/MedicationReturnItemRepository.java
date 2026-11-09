package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationReturn;
import com.eldercare.backend.medication.internal.handover.domain.MedicationReturnItem;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Dòng nhận lại nhúng trong {@code lines.items} của dòng {@code RETURN} (006 T069, {@code V006_97}). Chỉ đọc: dòng
 * nhận lại có đủ khi tạo {@link MedicationReturn} và khóa ngay khi thêm.
 */
@Repository
public class MedicationReturnItemRepository {

    private final MedicationReturnRepository returns;

    public MedicationReturnItemRepository(MedicationReturnRepository returns) {
        this.returns = returns;
    }

    public List<MedicationReturnItem> findByReturnId(Long returnId) {
        return returns.findById(returnId).map(MedicationReturn::getItems).orElseGet(List::of);
    }
}
