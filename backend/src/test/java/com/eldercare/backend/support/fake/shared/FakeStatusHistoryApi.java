package com.eldercare.backend.support.fake.shared;

import static com.eldercare.backend.support.fake.shared.FakeContent.condition;
import static com.eldercare.backend.support.fake.shared.FakeContent.valid;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Bản giả {@link StatusHistoryApi} giữ dữ liệu trong bộ nhớ (module-interfaces 000 mục 1.8). */
public class FakeStatusHistoryApi implements StatusHistoryApi {

    private record Row(String track, long entityId, HistoryRow<?> row) {}

    private final List<Row> rows = new ArrayList<>();
    private final Set<String> uniqueKeys = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized <D> long add(HistoryTrack<D> track, long entityId, HistoryEntry<D> e) {
        if (e.kind() == HistoryKind.PERIOD && open(track, entityId).isPresent()) {
            throw condition("OPEN_PERIOD", "Đang có khoảng chưa kết thúc");
        }
        if (e.uniqueKey() != null && !uniqueKeys.add(e.uniqueKey())) {
            throw condition("UNIQUE_KEY", "Trùng khóa duy nhất " + e.uniqueKey());
        }
        long id = FakeContent.IDS.incrementAndGet();
        rows.add(new Row(
                key(track),
                entityId,
                new HistoryRow<>(
                        id,
                        e.kind(),
                        e.valueBefore(),
                        e.value(),
                        e.valueId(),
                        e.startsAt(),
                        null,
                        e.command(),
                        e.actorId(),
                        e.bySystem(),
                        e.reason() == null ? null : e.reason().value(),
                        e.basisType(),
                        e.basisId(),
                        valid(e.details()),
                        e.floorAreaId())));
        return id;
    }

    @Override
    public synchronized <D> Optional<HistoryRow<D>> closeOpen(HistoryTrack<D> track, long entityId, Instant endsAt) {
        Optional<HistoryRow<D>> open = open(track, entityId);
        open.ifPresent(o -> {
            HistoryRow<D> closed = new HistoryRow<>(
                    o.id(),
                    o.kind(),
                    o.valueBefore(),
                    o.value(),
                    o.valueId(),
                    o.startsAt(),
                    endsAt,
                    o.command(),
                    o.actorId(),
                    o.bySystem(),
                    o.reason(),
                    o.basisType(),
                    o.basisId(),
                    o.details(),
                    o.floorAreaId());
            rows.replaceAll(r -> r.row().id() == o.id() ? new Row(r.track(), r.entityId(), closed) : r);
        });
        return open.map(o -> list(track, entityId).stream()
                .filter(r -> r.id() == o.id())
                .findFirst()
                .orElseThrow());
    }

    @Override
    public synchronized <D> Optional<HistoryRow<D>> open(HistoryTrack<D> track, long entityId) {
        return list(track, entityId).stream()
                .filter(r -> r.kind() == HistoryKind.PERIOD && r.endsAt() == null)
                .findFirst();
    }

    @Override
    public synchronized <D> Optional<HistoryRow<D>> at(HistoryTrack<D> track, long entityId, Instant at) {
        return list(track, entityId).stream()
                .filter(r -> !r.startsAt().isAfter(at))
                .filter(r -> r.kind() == HistoryKind.POINT
                        || r.endsAt() == null
                        || r.endsAt().isAfter(at))
                .reduce((first, second) -> second);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <D> List<HistoryRow<D>> list(HistoryTrack<D> track, long entityId) {
        return rows.stream()
                .filter(r -> r.track().equals(key(track)) && r.entityId() == entityId)
                .map(r -> (HistoryRow<D>) r.row())
                .sorted(Comparator.comparing((HistoryRow<D> r) -> r.startsAt()).thenComparingLong(HistoryRow::id))
                .toList();
    }

    private static String key(HistoryTrack<?> track) {
        return track.entityType() + "#" + track.attribute();
    }
}
