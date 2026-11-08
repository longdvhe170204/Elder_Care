package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.spi.ActivityHealthGatePort;
import com.eldercare.backend.support.fake.care.FakeActivityHealthGate;
import com.eldercare.backend.support.fake.care.FakeM04Switches;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class T021_ActivityHealthGateTest {
    @Test
    void FR_054_detectsInfectionAtAnyTimeWithinTheHalfOpenDay() throws Exception {
        var gate = new FakeActivityHealthGate(new FakeM04Switches());
        var from = Instant.parse("2026-11-07T17:00:00Z");
        var to = from.plusSeconds(86400);
        gate.suspectInfection(1L, from.plusSeconds(60), from.plusSeconds(120));
        ActivityHealthGatePort source = gate;
        assertThat(source.suspectedInfectionDuring(1L, from, to)).isTrue();
        assertThat(source.suspectedInfectionDuring(1L, from.plusSeconds(120), to))
                .isFalse();
        assertThat(source.suspectedInfectionDuring(1L, from.minusSeconds(10), from.plusSeconds(60)))
                .isFalse();
    }
}
