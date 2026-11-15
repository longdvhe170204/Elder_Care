package com.eldercare.backend.support.fake.medication;

import com.eldercare.backend.medication.api.DoseHandoverApi;
import com.eldercare.backend.medication.api.dto.DosePendingHandoverView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Nguồn liều dùng chung, không có lệnh chuyển hay vật hóa liều. */
public class FakeDoseHandover implements DoseHandoverApi {
    private record Scope(long floor, long shift) {}

    private final Map<Scope, List<DosePendingHandoverView>> rows = new java.util.concurrent.ConcurrentHashMap<>();
    private Runnable reading = () -> {};

    public void beforeRead(Runnable action) {
        reading = action;
    }

    public void source(long floor, long shift, List<DosePendingHandoverView> values) {
        rows.put(new Scope(floor, shift), List.copyOf(values));
    }

    public List<DosePendingHandoverView> inShift(long floor, long shift) {
        reading.run();
        return rows.getOrDefault(new Scope(floor, shift), List.of());
    }

    public Map<String, DoseStatus> currentStatuses(Collection<String> ids) {
        var result = new HashMap<String, DoseStatus>();
        rows.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> ids.contains(r.doseKey()))
                .forEach(r -> result.put(r.doseKey(), r.status()));
        return result;
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        FakeDoseHandover fakeDoseHandover() {
            return new FakeDoseHandover();
        }
    }
}
