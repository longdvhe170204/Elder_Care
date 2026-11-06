package com.eldercare.backend.belongings.internal.belonging.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingResidentState;
import org.springframework.stereotype.Repository;

/**
 * Khóa theo người cao tuổi của M12 ({@code do_gui_nct} → {@code resident_module_state} loại {@link #KIND}, R-15; research
 * K-03): lệnh trên đồ của một người khóa dòng này trước rồi mới khóa từng đồ.
 */
@Repository
public class BelongingResidentLock {

    static final SharedKind<BelongingResidentState> KIND =
            SharedKind.of("M12.BELONGING_RESIDENT", BelongingResidentState.class);

    private final ResidentModuleStateApi states;

    BelongingResidentLock(ResidentModuleStateApi states) {
        this.states = states;
    }

    public void lock(long residentId) {
        // Upsert trước SELECT FOR UPDATE để hai lệnh cùng tạo dòng không nâng khóa gap (mẫu FamilyResidentLock của 012)
        states.save(KIND, residentId, new BelongingResidentState());
        states.lockForUpdate(KIND, residentId);
    }
}
