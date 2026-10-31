package com.eldercare.backend.care.internal.status.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.care.internal.status.domain.CareResidentState;
import java.util.Collection;
import org.springframework.stereotype.Repository;

/** C-03: khóa tăng dần để các lô chồng nhau không đảo thứ tự khóa. */
@Repository
public class CareLockStore {
    private static final SharedKind<CareResidentState> STATE =
            SharedKind.of("M04.CARE_RESIDENT_STATE", CareResidentState.class);
    private final ResidentModuleStateApi states;

    public CareLockStore(ResidentModuleStateApi states) {
        this.states = states;
    }

    public void lock(Collection<Long> residentIds) {
        for (long id : residentIds.stream().distinct().sorted().toList()) {
            // Upsert trước SELECT FOR UPDATE để không nâng khóa gap/shared khi hai lệnh cùng tạo dòng.
            states.save(STATE, id, states.get(STATE, id).orElse(new CareResidentState(0)));
            states.lockForUpdate(STATE, id);
        }
    }
}
