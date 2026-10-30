package com.eldercare.backend.administration.shared.internal.service;

import static com.eldercare.backend.administration.shared.internal.service.SharedContent.condition;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.administration.shared.internal.domain.HistoryRecord;
import com.eldercare.backend.administration.shared.internal.persistence.StatusHistoryStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link StatusHistoryApi} trên {@code status_history} (nhóm 3, chỉ thêm; research R-15). */
@Service
class StatusHistoryService implements StatusHistoryApi {

    private final StatusHistoryStore store;
    private final SharedContent content;

    StatusHistoryService(StatusHistoryStore store, SharedContent content) {
        this.store = store;
        this.content = content;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <D> long add(HistoryTrack<D> track, long entityId, HistoryEntry<D> e) {
        String details = content.write(e.details());
        if (e.kind() == HistoryKind.PERIOD
                && store.lockOpen(track.entityType(), entityId, track.attribute())
                        .isPresent()) {
            throw condition("OPEN_PERIOD", "Đang có khoảng " + track.attribute() + " chưa kết thúc");
        }
        if (e.uniqueKey() != null && store.existsUniqueKey(e.uniqueKey())) {
            throw condition("UNIQUE_KEY", "Trùng khóa duy nhất " + e.uniqueKey());
        }
        HistoryRecord record = new HistoryRecord(
                0,
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
                details,
                e.floorAreaId());
        return store.insert(track.entityType(), entityId, track.attribute(), track.module(), record, e.uniqueKey());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <D> Optional<HistoryRow<D>> closeOpen(HistoryTrack<D> track, long entityId, Instant endsAt) {
        return store.lockOpen(track.entityType(), entityId, track.attribute()).map(open -> {
            store.close(open.id(), endsAt);
            return row(track, store.find(open.id()).orElseThrow());
        });
    }

    @Override
    public <D> Optional<HistoryRow<D>> open(HistoryTrack<D> track, long entityId) {
        return list(track, entityId).stream()
                .filter(r -> r.kind() == HistoryKind.PERIOD && r.endsAt() == null)
                .findFirst();
    }

    @Override
    public <D> Optional<HistoryRow<D>> at(HistoryTrack<D> track, long entityId, Instant at) {
        return store.at(track.entityType(), entityId, track.attribute(), at).map(r -> row(track, r));
    }

    @Override
    public <D> List<HistoryRow<D>> list(HistoryTrack<D> track, long entityId) {
        return store.list(track.entityType(), entityId, track.attribute()).stream()
                .map(r -> row(track, r))
                .toList();
    }

    private <D> HistoryRow<D> row(HistoryTrack<D> track, HistoryRecord r) {
        return new HistoryRow<>(
                r.id(),
                r.kind(),
                r.valueBefore(),
                r.value(),
                r.valueId(),
                r.startsAt(),
                r.endsAt(),
                r.command(),
                r.actorId(),
                r.bySystem(),
                r.reason(),
                r.basisType(),
                r.basisId(),
                content.read(track.details(), r.details()),
                r.floorAreaId());
    }
}
