package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.spi.VitalSignPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Fixture kết quả đồng bộ 007, không tự chấp nhận hay tính ngưỡng chỉ số. */
public class FakeVitalSigns implements VitalSignPort {
    private final FakeM04Switches switches;
    private final Map<Measurement, RecordingResult> results = new HashMap<>();
    private final List<Measurement> recordings = new ArrayList<>();

    public FakeVitalSigns(FakeM04Switches switches) {
        this.switches = switches;
    }

    public void result(Measurement input, RecordingResult result) {
        results.put(input, result);
    }

    @Override
    public RecordingResult record(
            long residentId,
            String vitalSignType,
            BigDecimal value,
            Instant measuredAt,
            long measuredById,
            long taskId,
            Optional<Instant> deviceAt) {
        switches.requireEnabled("007");
        var input = new Measurement(residentId, vitalSignType, value, measuredAt, measuredById, taskId, deviceAt);
        var result = results.get(input);
        if (result == null) throw new IllegalStateException("Chưa đặt kết quả giả cho lần đo");
        recordings.add(input);
        return result;
    }

    public List<Measurement> recordings() {
        return List.copyOf(recordings);
    }

    public void reset() {
        results.clear();
        recordings.clear();
    }

    public record Measurement(
            long residentId,
            String vitalSignType,
            BigDecimal value,
            Instant measuredAt,
            long measuredById,
            long taskId,
            Optional<Instant> deviceAt) {}
}
