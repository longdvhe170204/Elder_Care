package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.ResidentPositionState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * T066 (R-15; research G-03): vị trí theo người cao tuổi của M03 trên {@code resident_module_state} loại {@link #KIND}.
 * Khóa tuần tự theo người là dòng {@code technical_lock} (upsert lấy khóa X ngay như {@code resident_position} cũ, không
 * nâng khóa gap); đọc, ghi dữ liệu sau khi khóa, ghi qua {@link ResidentModuleStateApi}.
 */
@Repository
public class ResidentPositionStore {
    static final SharedKind<ResidentPositionState> KIND =
            SharedKind.of("M03.RESIDENT_POSITION", ResidentPositionState.class);

    private final ResidentModuleStateApi states;
    private final TechnicalLocks locks;
    private final JdbcTemplate jdbc;

    public ResidentPositionStore(ResidentModuleStateApi states, TechnicalLocks locks, JdbcTemplate jdbc) {
        this.states = states;
        this.locks = locks;
        this.jdbc = jdbc;
    }

    public void lock(long residentId) {
        locks.lock("M03.RESIDENT_POSITION:" + residentId);
    }

    /** Đọc không khóa (tra cứu). */
    public ResidentPositionState read(long residentId) {
        return states.get(KIND, residentId).orElse(ResidentPositionState.EMPTY);
    }

    /** Khóa người rồi đọc (trong lệnh). */
    public ResidentPositionState locked(long residentId) {
        lock(residentId);
        return read(residentId);
    }

    public Optional<Instant> withoutBedSince(long residentId) {
        return Optional.ofNullable(read(residentId).withoutBedSince());
    }

    /** Đặt dấu "chưa có giường" nếu chưa có; {@code true} khi vừa đặt. */
    public boolean markWithoutBed(long residentId, Instant at) {
        var state = locked(residentId);
        if (state.withoutBedSince() != null) return false;
        states.save(KIND, residentId, new ResidentPositionState(at, state.waitingHoldBedIds()));
        return true;
    }

    public void clearWithoutBed(long residentId) {
        var state = locked(residentId);
        if (state.withoutBedSince() != null)
            states.save(KIND, residentId, new ResidentPositionState(null, state.waitingHoldBedIds()));
    }

    public void waitingHoldBedIds(long residentId, List<Long> bedIds) {
        var state = locked(residentId);
        states.save(KIND, residentId, new ResidentPositionState(state.withoutBedSince(), bedIds));
    }

    /** Người đang mang dấu "chưa có giường", theo id. */
    public List<Long> residentsWithoutBed() {
        return jdbc.queryForList(
                "SELECT resident_id FROM resident_module_state WHERE module='M03'"
                        + " AND JSON_TYPE(data->'$.withoutBedSince')='STRING' ORDER BY resident_id",
                Long.class);
    }
}
