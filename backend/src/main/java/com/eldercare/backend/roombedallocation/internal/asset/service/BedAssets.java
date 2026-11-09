package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T020, hợp đồng 003 mục 3: bản cài {@link BedAssetPort}. Dấu "chờ chuyển người" đọc từ {@code asset}. Ghi tăng tài
 * sản giường kèm lệnh Tạo giường (FR-011, Q-238) cần id bộ phận Chăm sóc mà 008 chưa cung cấp cách tra; chủ dự án chọn
 * sau (2026-11-06), tới đó trả 503 và lệnh Tạo giường không ghi.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedAssets implements BedAssetPort {
    private final AssetStore store;
    private final RecordEventApi records;
    private final StatusHistoryApi history;
    private final LocationStructureApi structure;
    private final CurrentUser user;
    private final BusinessTime time;
    private final AuditLogApi audit;

    public BedAssets(
            AssetStore store,
            RecordEventApi records,
            StatusHistoryApi history,
            LocationStructureApi structure,
            CurrentUser user,
            BusinessTime time,
            AuditLogApi audit) {
        this.store = store;
        this.records = records;
        this.history = history;
        this.structure = structure;
        this.user = user;
        this.time = time;
        this.audit = audit;
    }

    @Override
    public void recordIncreaseForBed(long bedId) {
        throw new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SOURCE_UNAVAILABLE",
                "Chưa có cách xác định bộ phận Chăm sóc cho tài sản giường (FR-011)");
    }

    /**
     * 003 T017 (Q-243, 019 FR-012): đóng khoảng vị trí đang mở, mở khoảng mới ở phòng mới như lệnh chuyển vị trí của 019;
     * tài sản Đã thanh lý không đổi vị trí (409 {@code STATE_CHANGED}, như {@code AssetService.relocate}).
     */
    @Override
    public void relocateBedAsset(long bedId, long toRoomId) {
        for (long assetId : store.assetsOfBeds(Set.of(bedId)).keySet()) {
            var row = store.lock(assetId).orElseThrow(NotFoundException::new);
            if (row.status() == AssetStatus.DISPOSED)
                throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Tài sản giường đã thanh lý");
            long floor = structure
                    .room(toRoomId)
                    .filter(r -> r.active())
                    .orElseThrow(() -> new InvalidValueException("Phòng không tồn tại hoặc đã ngừng"))
                    .floorId();
            var now = time.now();
            history.closeOpen(AssetKinds.LOCATION, assetId, now);
            history.add(
                    AssetKinds.LOCATION,
                    assetId,
                    new HistoryEntry<>(
                            HistoryKind.PERIOD,
                            row.locationType(),
                            "ROOM",
                            toRoomId,
                            now,
                            "RELOCATE",
                            user.staffId().orElse(null),
                            false,
                            null,
                            "M03.BED",
                            bedId,
                            new AssetKinds.Location("ROOM", toRoomId, null),
                            floor,
                            null));
            store.updateLocation(assetId, row.status(), "ROOM", toRoomId, floor);
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("M03_ASSET_RELOCATE", false))
                    .entity(AssetKinds.ENTITY, assetId)
                    .before(Map.of("roomId", String.valueOf(row.locationId())))
                    .after(Map.of("roomId", String.valueOf(toRoomId)))
                    .build());
        }
    }

    @Override
    public boolean awaitingResidentTransfer(long bedId) {
        return !store.flaggedBeds(Set.of(bedId)).isEmpty();
    }

    @Override
    public Set<Long> flaggedBeds(Collection<Long> bedIds) {
        return store.flaggedBeds(bedIds);
    }

    @Override
    public Set<Long> flaggedBedsAt(Collection<Long> bedIds, java.time.Instant at) {
        var flagged = new java.util.HashSet<Long>();
        store.assetsOfBeds(bedIds).forEach((assetId, bedId) -> {
            boolean awaiting = records.list(AssetKinds.DAMAGE, new EntityRef(AssetKinds.ENTITY, assetId)).stream()
                    .anyMatch(e -> Boolean.TRUE.equals(e.content().awaitingTransfer())
                            && !e.occurredAt().isAfter(at)
                            && (e.confirmedAt() == null || e.confirmedAt().isAfter(at)));
            if (awaiting) flagged.add(bedId);
        });
        return Set.copyOf(flagged);
    }
}
