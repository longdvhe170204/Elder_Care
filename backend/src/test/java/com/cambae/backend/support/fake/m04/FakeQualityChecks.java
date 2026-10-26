package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.spi.QualityCheckPort;
import java.util.*;

/** Fixture kết quả Không đạt của 014, không suy đoán kết quả chưa được đặt. */
public class FakeQualityChecks implements QualityCheckPort {
    private final FakeM04Switches switches;
    private final Map<CheckTask, FailedCheckView> results = new HashMap<>();

    public FakeQualityChecks(FakeM04Switches switches) {
        this.switches = switches;
    }

    public void failed(long resultId, long originalTaskId, FailedCheckView value) {
        results.put(new CheckTask(resultId, originalTaskId), value);
    }

    @Override
    public Optional<FailedCheckView> failed(long resultId, long originalTaskId) {
        switches.requireEnabled("014");
        return Optional.ofNullable(results.get(new CheckTask(resultId, originalTaskId)));
    }

    public void reset() {
        results.clear();
    }

    private record CheckTask(long resultId, long originalTaskId) {}
}
