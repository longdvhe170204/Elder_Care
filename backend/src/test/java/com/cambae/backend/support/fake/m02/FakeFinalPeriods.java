package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m02.api.spi.FinalPeriodPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Bản giả {@link FinalPeriodPort} (010 cài): ghi lại lời gọi để test kiểm; {@link #failOnCall()} để thử rollback. */
public class FakeFinalPeriods implements FinalPeriodPort {

    private final List<String> calls = new ArrayList<>();
    private long nextId = 1;
    private boolean fail;

    public List<String> calls() {
        return List.copyOf(calls);
    }

    public FakeFinalPeriods failOnCall() {
        fail = true;
        return this;
    }

    public FakeFinalPeriods reset() {
        calls.clear();
        fail = false;
        return this;
    }

    private void record(String call) {
        if (fail) {
            throw new IllegalStateException("FINAL_PERIOD_FAILED");
        }
        calls.add(call);
    }

    @Override
    public long createDraft(long residentId, LocalDate expectedEndDate) {
        record("createDraft:" + residentId + ":" + expectedEndDate);
        return nextId++;
    }

    @Override
    public void recalculate(long residentId, LocalDate newDate) {
        record("recalculate:" + residentId + ":" + newDate);
    }

    @Override
    public void cancelStop(long residentId) {
        record("cancelStop:" + residentId);
    }
}
