package com.cambae.backend.support.fake.m05;

import com.cambae.backend.m05.api.AlertHandoverApi;
import com.cambae.backend.m05.api.dto.AlertHandoverView;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.*;

/** Một nguồn giả dùng chung của 007, chỉ nạp qua Config trong test. */
public class FakeAlertHandover implements AlertHandoverApi {
    private record Scope(long floor, long shift) {}

    private final Map<Scope, List<AlertHandoverView>> rows = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean fail;
    private Runnable confirmed = () -> {};

    public void source(long floor, long shift, List<AlertHandoverView> values) {
        rows.put(new Scope(floor, shift), List.copyOf(values));
    }

    public List<AlertHandoverView> open(long floor, long shift) {
        return rows.getOrDefault(new Scope(floor, shift), List.of());
    }

    public Map<SourceRef, AlertHandoverView> currentStatus(Collection<SourceRef> refs) {
        var result = new HashMap<SourceRef, AlertHandoverView>();
        rows.values().stream().flatMap(Collection::stream).forEach(r -> {
            var ref = new SourceRef(r.group(), r.id());
            if (refs.contains(ref)) result.put(ref, r);
        });
        return result;
    }

    public void failOnConfirm(boolean value) {
        fail = value;
    }

    public void onConfirm(Runnable action) {
        confirmed = action;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void onHandoverConfirmed(long h, long previous, Long next, long floor) {
        if (fail) throw new IllegalStateException("Nguồn cảnh báo từ chối xác nhận");
        confirmed.run();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        FakeAlertHandover fakeAlertHandover() {
            return new FakeAlertHandover();
        }
    }
}
