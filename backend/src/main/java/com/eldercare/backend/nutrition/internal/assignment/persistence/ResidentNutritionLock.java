package com.eldercare.backend.nutrition.internal.assignment.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.nutrition.internal.assignment.domain.ResidentNutritionState;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa {@code dinh_duong_nct} của một người cao tuổi (research M-03; R-15: dòng {@code resident_module_state} loại
 * {@code M08.RESIDENT_NUTRITION}): mọi lệnh đổi bản gán, đồ ăn gia đình của người đó chạy lần lượt.
 */
@Repository
public class ResidentNutritionLock {

    static final SharedKind<ResidentNutritionState> KIND =
            SharedKind.of("M08.RESIDENT_NUTRITION", ResidentNutritionState.class);

    private final ResidentModuleStateApi states;

    ResidentNutritionLock(ResidentModuleStateApi states) {
        this.states = states;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long residentId) {
        // Upsert trước SELECT FOR UPDATE để hai lệnh cùng tạo dòng không bế tắc (mẫu CareLockStore của 005)
        states.save(KIND, residentId, states.get(KIND, residentId).orElse(new ResidentNutritionState(0)));
        states.lockForUpdate(KIND, residentId);
    }
}
