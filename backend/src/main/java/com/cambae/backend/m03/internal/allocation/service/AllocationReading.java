package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.common.error.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m03.api.spi.AllocationResidencySource;
import com.cambae.backend.m03.internal.allocation.domain.*;
import com.cambae.backend.m03.internal.allocation.persistence.AllocationReadStore;
import com.cambae.backend.m03.internal.bed.domain.BedDetails;
import com.cambae.backend.m03.internal.bed.persistence.BedStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T027: quyền theo khóa và đối tượng cụ thể; không suy quyền từ tên vai trò. */
@Service
@Transactional(readOnly = true)
public class AllocationReading {
    private final AllocationReadStore store;
    private final BedStore beds;
    private final AllocationChecker checker;
    private final ResidentApi residents;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final BusinessTime time;
    private final CurrentUser user;
    private final PermissionApi permissions;

    public AllocationReading(
            AllocationReadStore store,
            BedStore beds,
            AllocationChecker checker,
            ResidentApi residents,
            ObjectProvider<AllocationResidencySource> residency,
            BusinessTime time,
            CurrentUser user,
            PermissionApi permissions) {
        this.store = store;
        this.beds = beds;
        this.checker = checker;
        this.residents = residents;
        this.residency = residency;
        this.time = time;
        this.user = user;
        this.permissions = permissions;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.VIEW)
    public List<AllocationDetails> list(Long residentId, Long bedId, AllocationStatus status) {
        return store.list(residentId, bedId, status).stream()
                .filter(this::visible)
                .toList();
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.VIEW)
    public AllocationDetails detail(long id) {
        return store.list(null, null, null).stream()
                .filter(row -> row.id() == id && visible(row))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    private boolean visible(AllocationDetails row) {
        return permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.VIEW),
                        new Target(
                                "M03.BED_ALLOCATION",
                                Long.toString(row.id()),
                                row.residentId(),
                                row.location().floorId()))
                .allowed();
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.VIEW)
    public List<BedDetails> suitable(
            long residentId, Instant startAt, Long fromAllocationId, boolean infectionControl) {
        Instant start = startAt == null ? time.now() : startAt;
        boolean future =
                fromAllocationId == null && residents.status(residentId) == ResidentStatus.ADMISSION_IN_PROGRESS;
        if (fromAllocationId != null && detail(fromAllocationId).residentId() != residentId)
            throw new NotFoundException();
        var source = residency.getIfAvailable();
        if (source == null)
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn lưu trú");
        Instant end = source.expectedEnd(residentId).orElse(null);
        return beds.list(null, null, null, null).stream()
                .filter(bed -> permissions
                        .hasPermission(
                                user,
                                new PermissionKey(Function.BED_ALLOCATION, Operation.VIEW),
                                new Target(
                                        "M03.BED",
                                        Long.toString(bed.id()),
                                        null,
                                        bed.location().floorId()))
                        .allowed())
                .filter(bed -> checker.check(residentId, bed.id(), start, end, fromAllocationId, future).stream()
                        .allMatch(condition ->
                                infectionControl && condition.code().equals("ALLOCATION_ISOLATION")))
                .sorted(java.util.Comparator.comparingLong(
                                (BedDetails bed) -> bed.location().areaId())
                        .thenComparingLong(bed -> bed.location().floorId())
                        .thenComparingLong(BedDetails::roomId)
                        .thenComparingLong(BedDetails::id))
                .toList();
    }
}
