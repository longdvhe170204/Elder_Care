package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocationReferences;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T015: bản giả dùng chung của SPI được phép khai báo cho C/D, không dùng trong ứng dụng thật. */
class ShiftPortFakesTest {
    private final Instant from = Instant.parse("2026-11-01T00:00:00Z");
    private final Instant to = from.plusSeconds(3600);

    @Test
    void FR_005_locationFakeRetainsPastReferenceAfterActiveReferenceEnds() {
        var fake = new FakeLocationReferences();
        var location = new LocationRef("FLOOR", 2);
        fake.references(location, List.of(new LocationReferenceView("M09.SHIFT", 1)));
        fake.references(location, List.of());
        assertThat(fake.currentReferences(location)).isEmpty();
        assertThat(fake.everReferenced(location)).isTrue();
        assertThat(fake.everReferenced(new LocationRef("FLOOR", 99))).isFalse();
    }
}
