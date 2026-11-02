package com.eldercare.backend.admission.internal.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.admission.internal.domain.ResidencyState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Đọc, ghi {@link ResidencyState} qua {@link ResidentModuleStateApi}; hàm ghi gọi sau khi đã khóa theo người. */
@Repository
public class ResidencyStateStore {

    private final ResidentModuleStateApi states;

    ResidencyStateStore(ResidentModuleStateApi states) {
        this.states = states;
    }

    public ResidencyState get(long residentId) {
        return states.get(ResidencyState.KIND, residentId).orElse(ResidencyState.empty());
    }

    public void save(long residentId, ResidencyState state) {
        states.save(ResidencyState.KIND, residentId, state);
    }

    public void recordAdmittedOn(long residentId, LocalDate on) {
        ResidencyState state = get(residentId);
        if (state.admittedOn() == null) {
            save(residentId, state.withAdmittedOn(on));
        }
    }

    public Optional<LocalDate> admittedOn(long residentId) {
        return Optional.ofNullable(get(residentId).admittedOn());
    }

    public Optional<Instant> closedAt(long residentId) {
        return Optional.ofNullable(get(residentId).closedAt());
    }

    /** Đặt dấu Đã đóng một lần; dấu đã có thì không đổi (FR-072). Trả {@code true} khi vừa đặt. */
    public boolean close(long residentId, Instant at) {
        ResidencyState state = get(residentId);
        if (state.closedAt() != null) {
            return false;
        }
        save(residentId, state.withClosedAt(at));
        return true;
    }
}
