package com.eldercare.backend.nutrition.internal.serving.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.serving.domain.ServingConfirmation;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Xác nhận phục vụ ở bảng chung {@code record_event} (R-15; cha là suất đặc biệt); ghi một lần rồi khóa dòng. */
@Repository
public class ServingRecordStore {

    private final RecordEventApi events;

    ServingRecordStore(RecordEventApi events) {
        this.events = events;
    }

    static EntityRef special(long specialId) {
        return new EntityRef(SpecialPortion.KIND.code(), specialId);
    }

    /** {@code deviceAt}: thời điểm thiết bị ghi khi ngoại tuyến (Q-01), lưu riêng với thời điểm máy chủ. */
    public RecordEvent<ServingConfirmation> add(ServingConfirmation content, long actor, Instant at, Instant deviceAt) {
        return events.add(
                ServingConfirmation.KIND,
                special(content.specialPortionId()),
                new NewRecordEvent<>(content, null, actor, false, at, deviceAt, at, content.floorAreaId(), null));
    }

    public List<RecordEvent<ServingConfirmation>> of(long specialId) {
        return events.list(ServingConfirmation.KIND, special(specialId));
    }
}
