package com.eldercare.backend.admission.internal.waitinglist.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContact;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Lần liên hệ giường của một hồ sơ chờ qua {@link RecordEventApi}; chỉ thêm (DBR-23). */
@Repository
public class BedContactStore {

    /** Loại đối tượng cha, trùng {@code RetentionProvider.dataType()} của hồ sơ chờ để xóa cùng chuỗi. */
    public static final String ENTITY_TYPE = "M02.WAITING_LIST_ENTRY";

    private final RecordEventApi events;

    BedContactStore(RecordEventApi events) {
        this.events = events;
    }

    /** {@code actorId} rỗng khi hệ thống ghi. */
    public void add(long entryId, BedContact contact, Long actorId, Instant at) {
        events.add(
                BedContact.KIND,
                new EntityRef(ENTITY_TYPE, entryId),
                new NewRecordEvent<>(contact, null, actorId, actorId == null, at, null, at, null, null));
    }

    public List<RecordEvent<BedContact>> of(long entryId) {
        return events.list(BedContact.KIND, new EntityRef(ENTITY_TYPE, entryId));
    }
}
