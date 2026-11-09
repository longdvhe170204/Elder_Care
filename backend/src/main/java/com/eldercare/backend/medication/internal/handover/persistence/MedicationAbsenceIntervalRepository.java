package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.medication.internal.governance.persistence.RecordActor;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalContent;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khoảng vắng trên {@code simple_record} loại {@code M07.ABSENCE_INTERVAL} (006 T069, {@code V006_98}). Trạng thái
 * {@code OPEN}, {@code CLOSED}; khoảng mở giữ {@code unique_key} {@code M07.AI:<residentId>} (thay
 * {@code uq_open_absence_resident}), khi đóng thì nhả.
 */
@Repository
@Transactional
public class MedicationAbsenceIntervalRepository {

    private static final Set<String> ALL = Set.of("OPEN", "CLOSED");

    private final SimpleRecordApi rows;
    private final RecordActor actor;

    public MedicationAbsenceIntervalRepository(SimpleRecordApi rows, RecordActor actor) {
        this.rows = rows;
        this.actor = actor;
    }

    private static String openKey(long residentId) {
        return "M07.AI:" + residentId;
    }

    private static MedicationAbsenceInterval toDomain(SimpleRecord<AbsenceIntervalContent> row) {
        MedicationAbsenceInterval interval = MedicationAbsenceInterval.restore(row.residentId(), row.content());
        interval.restoreRow(row.id(), row.version(), row.createdAt());
        return interval;
    }

    public MedicationAbsenceInterval save(MedicationAbsenceInterval interval) {
        String status = interval.isOpen() ? "OPEN" : "CLOSED";
        String key = interval.isOpen() ? openKey(interval.getResidentId()) : null;
        SimpleRecord<AbsenceIntervalContent> row = interval.getId() == null
                ? rows.create(
                        AbsenceIntervalContent.KIND,
                        new NewSimpleRecord<>(
                                interval.getResidentId(),
                                new EntityRef("M01.RESIDENT", interval.getResidentId()),
                                status,
                                interval.content(),
                                null,
                                null,
                                key,
                                actor.accountId(),
                                false))
                : rows.change(
                                AbsenceIntervalContent.KIND,
                                interval.getId(),
                                interval.getVersion(),
                                new RecordChange<>(status, interval.content(), null, key, false))
                        .after();
        interval.restoreRow(row.id(), row.version(), row.createdAt());
        return interval;
    }

    public Optional<MedicationAbsenceInterval> findOpenByResidentId(Long residentId) {
        return rows.findByUniqueKey(AbsenceIntervalContent.KIND, openKey(residentId))
                .map(MedicationAbsenceIntervalRepository::toDomain);
    }

    /** Mọi khoảng vắng của người cao tuổi, mở và đã đóng (đầu vào lịch thuốc, research D-06). */
    public List<MedicationAbsenceInterval> findByResidentId(Long residentId) {
        return rows.listByResident(AbsenceIntervalContent.KIND, residentId, ALL).stream()
                .map(MedicationAbsenceIntervalRepository::toDomain)
                .toList();
    }

    public Optional<MedicationAbsenceInterval> findByTripId(Long tripId) {
        return rows.listByStatus(AbsenceIntervalContent.KIND, ALL).stream()
                .filter(row -> Objects.equals(row.content().tripId(), tripId))
                .map(MedicationAbsenceIntervalRepository::toDomain)
                .findFirst();
    }
}
