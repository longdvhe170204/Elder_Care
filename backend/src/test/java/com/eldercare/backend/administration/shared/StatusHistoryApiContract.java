package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.OTHER_TRACK;
import static com.eldercare.backend.administration.shared.SharedTestKinds.TRACK;
import static com.eldercare.backend.administration.shared.SharedTestKinds.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.SharedTestKinds.Attributes;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Hợp đồng của {@link StatusHistoryApi} (module-interfaces 000 mục 1.8), chạy trên bản thật và bản giả. */
public interface StatusHistoryApiContract {

    StatusHistoryApi history();

    <R> R tx(Supplier<R> work);

    Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static HistoryEntry<Attributes> entry(HistoryKind kind, String value, Instant startsAt, String uniqueKey) {
        return new HistoryEntry<>(
                kind,
                null,
                value,
                null,
                startsAt,
                "CHANGE",
                7L,
                false,
                new Reason("lý do"),
                "APPROVAL_REQUEST",
                3L,
                new Attributes("đỏ", 1),
                null,
                uniqueKey);
    }

    @Test
    default void points_areListedInOrder_withDetails() {
        long entity = TestUsers.nextId();
        tx(() -> history().add(TRACK, entity, entry(HistoryKind.POINT, "B", T0.plus(1, ChronoUnit.DAYS), null)));
        tx(() -> history().add(TRACK, entity, entry(HistoryKind.POINT, "A", T0, null)));

        assertThat(tx(() -> history().list(TRACK, entity)))
                .extracting(HistoryRow::value)
                .containsExactly("A", "B");
        HistoryRow<Attributes> first = tx(() -> history().list(TRACK, entity)).getFirst();
        assertThat(first.details()).isEqualTo(new Attributes("đỏ", 1));
        assertThat(first.reason()).isEqualTo("lý do");
        assertThat(first.basisId()).isEqualTo(3L);
        assertThat(first.endsAt()).isNull();
        // Thuộc tính khác của cùng đối tượng là lịch sử riêng
        assertThat(tx(() -> history().list(OTHER_TRACK, entity))).isEmpty();
    }

    @Test
    default void period_onlyOneOpen_closeOnce() {
        long entity = TestUsers.nextId();
        tx(() -> history().add(TRACK, entity, entry(HistoryKind.PERIOD, "A", T0, null)));

        assertThatThrownBy(() -> tx(() -> history()
                        .add(TRACK, entity, entry(HistoryKind.PERIOD, "B", T0.plus(1, ChronoUnit.DAYS), null))))
                .isInstanceOf(ConditionNotMetException.class);

        Instant end = T0.plus(2, ChronoUnit.DAYS);
        assertThat(tx(() -> history().closeOpen(TRACK, entity, end)))
                .hasValueSatisfying(row -> assertThat(row.endsAt()).isEqualTo(end));
        assertThat(tx(() -> history().open(TRACK, entity))).isEmpty();
        assertThat(tx(() -> history().closeOpen(TRACK, entity, end))).isEmpty();

        tx(() -> history().add(TRACK, entity, entry(HistoryKind.PERIOD, "B", end, null)));
        assertThat(tx(() -> history().open(TRACK, entity)))
                .hasValueSatisfying(row -> assertThat(row.value()).isEqualTo("B"));
    }

    @Test
    default void at_returnsRowInEffectAtInstant() {
        long entity = TestUsers.nextId();
        Instant end = T0.plus(10, ChronoUnit.DAYS);
        tx(() -> history().add(TRACK, entity, entry(HistoryKind.PERIOD, "A", T0, null)));
        tx(() -> history().closeOpen(TRACK, entity, end));
        tx(() -> history().add(TRACK, entity, entry(HistoryKind.PERIOD, "B", end, null)));

        assertThat(tx(() -> history().at(TRACK, entity, T0.minusSeconds(1)))).isEmpty();
        assertThat(tx(() -> history().at(TRACK, entity, T0.plus(5, ChronoUnit.DAYS))))
                .hasValueSatisfying(row -> assertThat(row.value()).isEqualTo("A"));
        assertThat(tx(() -> history().at(TRACK, entity, end)))
                .hasValueSatisfying(row -> assertThat(row.value()).isEqualTo("B"));
    }

    @Test
    default void uniqueKey_isEnforced() {
        String key = unique("K");
        tx(() -> history().add(TRACK, TestUsers.nextId(), entry(HistoryKind.POINT, "A", T0, key)));

        assertThatThrownBy(() ->
                        tx(() -> history().add(TRACK, TestUsers.nextId(), entry(HistoryKind.POINT, "B", T0, key))))
                .isInstanceOf(ConditionNotMetException.class);
    }
}
