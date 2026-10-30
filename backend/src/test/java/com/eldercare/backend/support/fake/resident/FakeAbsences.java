package com.eldercare.backend.support.fake.resident;

import com.eldercare.backend.resident.api.dto.OpenAbsence;
import com.eldercare.backend.resident.api.spi.AbsenceApi;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link AbsenceApi} (004 cài): lượt Tạm vắng đang mở do test đặt sẵn. */
public class FakeAbsences implements AbsenceApi {

    private final Map<Long, OpenAbsence> open = new HashMap<>();
    private boolean available = true;

    public FakeAbsences available(boolean value) {
        available = value;
        return this;
    }

    public FakeAbsences open(long residentId, OpenAbsence absence) {
        open.put(residentId, absence);
        return this;
    }

    public FakeAbsences close(long residentId) {
        open.remove(residentId);
        return this;
    }

    @Override
    public Optional<OpenAbsence> open(long residentId) {
        if (!available) throw new IllegalStateException("Nguồn tạm vắng chưa đọc được");
        return Optional.ofNullable(open.get(residentId));
    }
}
