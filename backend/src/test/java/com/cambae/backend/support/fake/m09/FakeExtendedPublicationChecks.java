package com.cambae.backend.support.fake.m09;

import com.cambae.backend.m09.shift.internal.service.port.ExtendedPublicationCheck;
import com.cambae.backend.m09.shift.internal.service.port.PublicationWarning;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test đặt cảnh báo theo lịch để mô phỏng 015, không quyết định nghỉ thay cho 015. */
public class FakeExtendedPublicationChecks implements ExtendedPublicationCheck {

    private final Map<Long, List<PublicationWarning>> warnings = new LinkedHashMap<>();
    private boolean available = true;

    public FakeExtendedPublicationChecks warnings(long scheduleId, List<PublicationWarning> value) {
        warnings.put(scheduleId, List.copyOf(value));
        return this;
    }

    public FakeExtendedPublicationChecks available(boolean value) {
        available = value;
        return this;
    }

    @Override
    public List<PublicationWarning> check(long scheduleId) {
        if (!available) {
            throw new IllegalStateException("Nguồn kiểm tra công bố giả chưa có dữ liệu");
        }
        return warnings.getOrDefault(scheduleId, List.of());
    }
}
