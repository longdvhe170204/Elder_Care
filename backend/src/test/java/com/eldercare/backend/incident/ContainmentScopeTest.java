package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eldercare.backend.incident.internal.infection.domain.ContainmentPeriod;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContainmentScopeTest {

    @Test
    void BR_M05_11_scopeAllowsSeveralFloorsOrOneArea() {
        assertThat(new ContainmentScope(Set.of(10L, 20L), Set.of()).floorIds()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(new ContainmentScope(Set.of(), Set.of(30L)).areaIds()).containsExactly(30L);

        assertThatIllegalArgumentException().isThrownBy(() -> new ContainmentScope(Set.of(), Set.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new ContainmentScope(Set.of(10L), Set.of(30L)));
        assertThatIllegalArgumentException().isThrownBy(() -> new ContainmentScope(Set.of(), Set.of(30L, 40L)));
    }

    @Test
    void BR_M05_12_scopeIsEffectiveFromCreationUntilRelease() {
        var start = Instant.parse("2026-11-03T00:00:00Z");
        var end = start.plusSeconds(600);
        var scope = new ContainmentScope(Set.of(10L), Set.of());
        var active = new ContainmentPeriod(scope, start, null);
        var released = new ContainmentPeriod(scope, start, end);

        assertThat(active.includes(start.minusNanos(1))).isFalse();
        assertThat(active.includes(end)).isTrue();
        assertThat(released.includes(start)).isTrue();
        assertThat(released.includes(end.minusNanos(1))).isTrue();
        assertThat(released.includes(end)).isFalse();
    }
}
