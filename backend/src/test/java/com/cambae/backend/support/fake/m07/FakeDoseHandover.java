package com.cambae.backend.support.fake.m07;

import com.cambae.backend.m07.api.DoseHandoverApi;
import com.cambae.backend.m07.api.dto.DoseHandoverView;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Nguồn liều dùng chung, không có lệnh chuyển hay vật hóa liều. */
public class FakeDoseHandover implements DoseHandoverApi {
    private record Scope(long floor, long shift) {}

    private final Map<Scope, List<DoseHandoverView>> rows = new java.util.concurrent.ConcurrentHashMap<>();
    private Runnable reading = () -> {};

    public void beforeRead(Runnable action) {
        reading = action;
    }

    public void source(long floor, long shift, List<DoseHandoverView> values) {
        rows.put(new Scope(floor, shift), List.copyOf(values));
    }

    public List<DoseHandoverView> inShift(long floor, long shift) {
        reading.run();
        return rows.getOrDefault(new Scope(floor, shift), List.of());
    }

    public Map<String, String> currentStatus(Collection<String> ids) {
        var result = new HashMap<String, String>();
        rows.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> ids.contains(r.key()))
                .forEach(r -> result.put(r.key(), r.status()));
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
