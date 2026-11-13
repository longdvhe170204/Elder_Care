package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.EVENT;
import static com.eldercare.backend.administration.shared.SharedTestKinds.OTHER_EVENT;
import static com.eldercare.backend.administration.shared.SharedTestKinds.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.SharedTestKinds.Attributes;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Hợp đồng của {@link RecordEventApi} (module-interfaces 000 mục 1.8), chạy trên bản thật và bản giả. */
public interface RecordEventApiContract {

    RecordEventApi events();

    <R> R tx(Supplier<R> work);

    Instant T0 = Instant.parse("2026-02-01T01:00:00Z");

    private static NewRecordEvent<Attributes> event(Instant at, Instant confirmedAt, Long replyTo, String key) {
        return new NewRecordEvent<>(
                new Attributes("đỏ", 1), replyTo, 5L, false, at, at.minusSeconds(60), confirmedAt, 9L, key);
    }

    private static EntityRef parent() {
        return new EntityRef("M15.TEST_ENTITY", TestUsers.nextId());
    }

    @Test
    default void add_thenListInOrder_withReply() {
        EntityRef parent = parent();
        RecordEvent<Attributes> later =
                tx(() -> events().add(EVENT, parent, event(T0.plusSeconds(10), T0, null, null)));
        RecordEvent<Attributes> earlier = tx(() -> events().add(EVENT, parent, event(T0, T0, null, null)));
        RecordEvent<Attributes> reply =
                tx(() -> events().add(EVENT, parent, event(T0.plusSeconds(20), T0, earlier.id(), null)));

        assertThat(tx(() -> events().list(EVENT, parent)))
                .extracting(RecordEvent::id)
                .containsExactly(earlier.id(), later.id(), reply.id());
        RecordEvent<Attributes> read = tx(() -> events().require(EVENT, reply.id()));
        assertThat(read).isEqualTo(reply);
        assertThat(read.replyToId()).isEqualTo(earlier.id());
        assertThat(read.parent()).isEqualTo(parent);
        assertThat(read.deviceAt()).isEqualTo(T0.plusSeconds(20).minusSeconds(60));
        assertThat(tx(() -> events().list(OTHER_EVENT, parent))).isEmpty();
    }

    @Test
    default void uniqueKey_secondAdd_isAlreadyProcessed() {
        String key = unique("E");
        RecordEvent<Attributes> first = tx(() -> events().add(EVENT, parent(), event(T0, T0, null, key)));

        assertThatThrownBy(() -> tx(() -> events().add(EVENT, parent(), event(T0, T0, null, key))))
                .isInstanceOf(AlreadyProcessedException.class);
        assertThat(tx(() -> events().findByUniqueKey(EVENT, key))).contains(first);
    }
}
