package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationCandidate;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCheckStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T025: một bộ kiểm FR-018 (a)–(i) cho danh sách chọn giường và mọi lệnh ghi. */
@Service
public class AllocationChecker {
    private final AllocationCheckStore store;
    private final ResidentAllocationFacts facts;
    private final ResidentApi residents;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final ObjectProvider<BedAssetPort> assets;

    public AllocationChecker(
            AllocationCheckStore store,
            ResidentAllocationFacts facts,
            ResidentApi residents,
            ObjectProvider<AllocationResidencySource> residency,
            ObjectProvider<AllocationInfectionStatus> infection,
            ObjectProvider<BedAssetPort> assets) {
        this.store = store;
        this.facts = facts;
        this.residents = residents;
        this.residency = residency;
        this.infection = infection;
        this.assets = assets;
    }

    @Transactional(readOnly = true)
    public List<UnmetCondition> check(
            long residentId, long bedId, Instant start, Instant end, Long excludedAllocationId, boolean future) {
        return check(residentId, bedId, start, end, excludedAllocationId, future, false);
    }

    /** Chỉ gọi sau khóa vị trí và toàn bộ giường, khi tham gia transaction RR của module nguồn. */
    public List<UnmetCondition> checkCurrent(
            long residentId, long bedId, Instant start, Instant end, Long excludedAllocationId, boolean future) {
        return check(residentId, bedId, start, end, excludedAllocationId, future, true);
    }

    private List<UnmetCondition> check(
            long residentId,
            long bedId,
            Instant start,
            Instant end,
            Long excludedAllocationId,
            boolean future,
            boolean current) {
        var source = residency.getIfAvailable();
        var cordons = infection.getIfAvailable();
        var asset = assets.getIfAvailable();
        if (source == null || cordons == null || asset == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa đủ nguồn kiểm phân bổ giường");
        var bed = current ? store.currentCandidate(bedId) : store.candidate(bedId);
        List<UnmetCondition> errors = new ArrayList<>();
        if (future
                && bed.state() == BedStatus.AWAITING_READY
                && (bed.readyDeadline() == null || start.isBefore(bed.readyDeadline())))
            errors.add(new UnmetCondition(
                    "ALLOCATION_NOT_READY_BEFORE_DEADLINE",
                    "Bắt đầu dự kiến phải từ hạn giường sẵn sàng",
                    "FR-011a",
                    null,
                    null));
        errors.addAll(policyConditions(residentId, bed));
        var cordon = cordons.cordonedAt(start);
        if (bed.isolated()
                || cordon.floorIds().contains(bed.floorId())
                || cordon.areaIds().contains(bed.areaId())) add(errors, "d", "Phòng đang chịu chặn cách ly");
        if (!bed.active()
                || !(bed.state() == BedStatus.EMPTY
                        || bed.state() == BedStatus.HELD
                        || future && bed.state() == BedStatus.AWAITING_READY))
            add(errors, "e", "Giường hoặc phòng chưa đủ điều kiện nhận phân bổ");
        if (bed.state() == BedStatus.HELD && !Objects.equals(bed.heldForResidentId(), residentId))
            add(errors, "f", "Giường đang giữ chỗ cho người hoặc hồ sơ khác");
        if (store.overlaps(bedId, start, end, excludedAllocationId, current))
            add(errors, "g", "Khoảng phân bổ chồng với phân bổ khác của giường");
        if (residents.isFinalStatus(residentId)) add(errors, "h", "Hồ sơ đã ở trạng thái cuối");
        if (asset.awaitingResidentTransfer(bedId)) add(errors, "i", "Tài sản giường đang chờ chuyển người");
        return List.copyOf(errors);
    }

    @Transactional(readOnly = true)
    public List<UnmetCondition> policyConditions(long residentId, long bedId) {
        return policyConditions(residentId, store.candidate(bedId));
    }

    private List<UnmetCondition> policyConditions(long residentId, AllocationCandidate bed) {
        var source = residency.getIfAvailable();
        if (source == null)
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn lưu trú");
        List<UnmetCondition> errors = new ArrayList<>();
        if (facts.careLevel(residentId)
                .filter(level -> bed.careLevels().contains(level.level()))
                .isEmpty()) add(errors, "a", "Mức chăm sóc không được phòng cho phép");
        var type = source.residencyType(residentId);
        if (type.isEmpty()
                || !(type.get() == ResidencyType.LONG_TERM && bed.servesLongTerm()
                        || type.get() == ResidencyType.SHORT_TERM && bed.servesShortTerm()))
            add(errors, "b", "Loại lưu trú không được loại phòng phục vụ");
        if (!facts.genderAllowed(residentId, bed.genderPolicy()))
            add(errors, "c", "Giới tính không phù hợp chính sách phòng");
        return List.copyOf(errors);
    }

    private static void add(List<UnmetCondition> errors, String clause, String message) {
        String code = switch (clause) {
            case "a" -> "CARE_LEVEL";
            case "b" -> "RESIDENCY_TYPE";
            case "c" -> "GENDER";
            case "d" -> "ISOLATION";
            case "e" -> "BED_STATE";
            case "f" -> "HOLD";
            case "g" -> "TIME_OVERLAP";
            case "h" -> "PROFILE_CLOSED";
            case "i" -> "ASSET_FLAG";
            default -> throw new IllegalArgumentException("Điều kiện phân bổ không hợp lệ");
        };
        errors.add(new UnmetCondition("ALLOCATION_" + code, message, "FR-018(" + clause + ")", null, null));
    }
}
