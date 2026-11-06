package com.eldercare.backend.resident.internal.health.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecord;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordData;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Hồ sơ sức khỏe ban đầu ở {@code simple_record} loại {@code M01.INITIAL_HEALTH_RECORD} (001 T084; thay
 * {@code initial_health_record}): ghi qua {@link SimpleRecordApi} trong transaction của lệnh; xác nhận khóa dòng.
 */
@Repository
public class InitialHealthRecordStore {

    private final SimpleRecordApi records;

    InitialHealthRecordStore(SimpleRecordApi records) {
        this.records = records;
    }

    public Optional<InitialHealthRecord> findByResidentId(long residentId) {
        return records.findByUniqueKey(InitialHealthRecordData.KIND, InitialHealthRecordData.uniqueKey(residentId))
                .map(InitialHealthRecordStore::record);
    }

    public Optional<InitialHealthRecord> findById(long id) {
        try {
            return Optional.of(record(records.require(InitialHealthRecordData.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Bản nháp mới; trùng hồ sơ của người cao tuổi thì 409 ({@code M01.IHR:<residentId>}). */
    public InitialHealthRecord create(InitialHealthRecord draft) {
        return record(records.create(
                InitialHealthRecordData.KIND,
                new NewSimpleRecord<>(
                        draft.getResidentId(),
                        null,
                        InitialHealthRecordData.DRAFT,
                        draft.data(),
                        null,
                        null,
                        InitialHealthRecordData.uniqueKey(draft.getResidentId()),
                        draft.getPerformedBy(),
                        false)));
    }

    /** Ghi bản nháp đã sửa hoặc đã xác nhận (khóa dòng); {@code version} là phiên bản lúc đọc (412 khi khác). */
    public InitialHealthRecord save(InitialHealthRecord record) {
        boolean confirm = record.isConfirmed();
        return record(records.change(
                        InitialHealthRecordData.KIND,
                        record.getId(),
                        record.getVersion(),
                        new RecordChange<>(
                                confirm ? InitialHealthRecordData.CONFIRMED : InitialHealthRecordData.DRAFT,
                                record.data(),
                                null,
                                InitialHealthRecordData.uniqueKey(record.getResidentId()),
                                confirm))
                .after());
    }

    private static InitialHealthRecord record(SimpleRecord<InitialHealthRecordData> row) {
        InitialHealthRecordData d = row.content();
        return new InitialHealthRecord(
                row.id(),
                row.residentId(),
                new InitialHealthRecordContent(
                        d.bloodType(),
                        d.mobility(),
                        d.selfCareAbility(),
                        d.cognition(),
                        d.initialNeedsNote(),
                        d.nutritionNeeds(),
                        d.currentMedications(),
                        d.rehabilitationNeeds()),
                d.performedBy(),
                d.performedAt(),
                d.confirmedBy(),
                row.lockedAt(),
                row.version());
    }
}
