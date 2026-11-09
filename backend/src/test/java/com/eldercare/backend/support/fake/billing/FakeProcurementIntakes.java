package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.billing.api.spi.ProcurementIntakePort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 019 khai {@link ProcurementIntakePort}, chủ fake là C; bản thật do D (006) cài ở M07. Mặc định chưa có lần tiếp nhận
 * Đạt nào; test đặt bằng {@link #passed}.
 */
public class FakeProcurementIntakes implements ProcurementIntakePort {
    private final Map<Long, Set<Long>> passed = new HashMap<>();

    public FakeProcurementIntakes passed(long procurementRequestId, long lineId) {
        passed.computeIfAbsent(procurementRequestId, r -> new HashSet<>()).add(lineId);
        return this;
    }

    @Override
    public Set<Long> passedLineIds(long procurementRequestId) {
        return Set.copyOf(passed.getOrDefault(procurementRequestId, Set.of()));
    }

    public void reset() {
        passed.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeProcurementIntakes fakeProcurementIntakes() {
            return new FakeProcurementIntakes();
        }
    }
}
