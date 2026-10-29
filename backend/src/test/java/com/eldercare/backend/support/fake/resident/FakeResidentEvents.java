package com.eldercare.backend.support.fake.resident;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Cách phát sự kiện của M01 trong test của module khác (002 T094 nghe sang trạng thái cuối để khóa tài khoản): gọi
 * {@link #statusChanged} thay cho lệnh đổi trạng thái thật. Gọi trong transaction của test nếu listener chạy sau commit.
 */
public class FakeResidentEvents {

    private final ApplicationEventPublisher publisher;
    private long nextHistoryId = 1;

    public FakeResidentEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public ResidentStatusChanged statusChanged(
            long residentId, ResidentStatus from, ResidentStatus to, StatusCommand command, Instant at) {
        ResidentStatusChanged event = new ResidentStatusChanged(residentId, from, to, command, nextHistoryId++, at);
        publisher.publishEvent(event);
        return event;
    }
}
