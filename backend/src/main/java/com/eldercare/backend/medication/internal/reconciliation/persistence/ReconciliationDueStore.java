package com.eldercare.backend.medication.internal.reconciliation.persistence;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationResidentState;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hạn đối chiếu thuốc theo người cao tuổi ({@code thuoc_nct.han_doi_chieu}, research D-13) trong
 * {@code resident_module_state} của M07: đặt khi Hoàn tất tiếp nhận hoặc Ghi nhận trở về từ bệnh viện, gỡ khi phiếu được
 * xác nhận; cảnh báo quá hạn một lần ({@code reconciliationWarnedAt}).
 */
@Component
public class ReconciliationDueStore {

    private final ResidentModuleStateApi states;
    private final ResidentMedicationLock lock;
    private final JdbcTemplate jdbc;

    public ReconciliationDueStore(ResidentModuleStateApi states, ResidentMedicationLock lock, JdbcTemplate jdbc) {
        this.states = states;
        this.lock = lock;
        this.jdbc = jdbc;
    }

    public void start(long residentId, Instant dueAt, String reason) {
        lock.lock(residentId);
        states.save(MedicationResidentState.KIND, residentId, MedicationResidentState.dueAt(dueAt, reason));
    }

    public void clear(long residentId) {
        lock.lock(residentId);
        states.save(MedicationResidentState.KIND, residentId, MedicationResidentState.empty());
    }

    public void markWarned(long residentId, Instant warnedAt) {
        lock.lock(residentId);
        states.get(MedicationResidentState.KIND, residentId)
                .ifPresent(state -> states.save(MedicationResidentState.KIND, residentId, state.warnedAt(warnedAt)));
    }

    /** Người cao tuổi tới hạn đối chiếu mà chưa được cảnh báo (kể cả khi chưa lập phiếu). */
    public List<Long> overdueResidentIds(Instant now) {
        return jdbc
                .queryForList("SELECT resident_id FROM resident_module_state WHERE module = 'M07'", Long.class)
                .stream()
                .filter(id -> states.get(MedicationResidentState.KIND, id)
                        .filter(s -> s.reconciliationDueAt() != null
                                && !s.reconciliationDueAt().isAfter(now)
                                && s.reconciliationWarnedAt() == null)
                        .isPresent())
                .toList();
    }
}
