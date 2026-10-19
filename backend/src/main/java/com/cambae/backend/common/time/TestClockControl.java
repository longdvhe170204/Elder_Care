package com.cambae.backend.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Đồng hồ giả lập (NFR-13, research R-04), chỉ có ở profile {@code test}, {@code demo}: chạy theo đồng hồ máy cộng
 * một độ lệch; đặt hay tua là đổi độ lệch.
 */
@Component
@Profile({"test", "demo"})
public class TestClockControl extends Clock {

    private final Clock system = Clock.system(BusinessTime.ZONE);
    private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

    /** Đặt thời điểm hiện tại; đồng hồ chạy tiếp từ mốc này. */
    public void setTo(Instant instant) {
        offset.set(Duration.between(system.instant(), instant));
    }

    /** Tua tới (hoặc lùi nếu âm) một khoảng. */
    public void advanceBy(Duration duration) {
        offset.updateAndGet(current -> current.plus(duration));
    }

    /** Về đồng hồ thật. */
    public void reset() {
        offset.set(Duration.ZERO);
    }

    public boolean isSimulated() {
        return !offset.get().isZero();
    }

    @Override
    public Instant instant() {
        return system.instant().plus(offset.get());
    }

    @Override
    public ZoneId getZone() {
        return BusinessTime.ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        // Múi giờ khác: bản chụp độ lệch hiện tại; mã nghiệp vụ dùng BusinessTime nên không đi nhánh này
        return zone.equals(getZone()) ? this : Clock.offset(Clock.system(zone), offset.get());
    }
}
