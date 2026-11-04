package com.eldercare.backend.family.internal.relationship.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.family.internal.relationship.domain.FamilyResidentState;
import org.springframework.stereotype.Repository;

/**
 * Khóa theo người cao tuổi của M10 ({@code nguoi_than_nct} → {@code resident_module_state} loại {@link #KIND}, R-15;
 * research F-03): mọi lệnh đổi quan hệ, vai trò, quyền của một người cao tuổi khóa dòng này tới hết transaction.
 */
@Repository
public class FamilyResidentLock {

    static final SharedKind<FamilyResidentState> KIND =
            SharedKind.of("M10.FAMILY_RESIDENT_STATE", FamilyResidentState.class);

    private final ResidentModuleStateApi states;

    FamilyResidentLock(ResidentModuleStateApi states) {
        this.states = states;
    }

    public void lock(long residentId) {
        // Upsert trước SELECT FOR UPDATE để hai lệnh cùng tạo dòng không nâng khóa gap (mẫu CareLockStore của 005)
        states.save(KIND, residentId, new FamilyResidentState());
        states.lockForUpdate(KIND, residentId);
    }
}
