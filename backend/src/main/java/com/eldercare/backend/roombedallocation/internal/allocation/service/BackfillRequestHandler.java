package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCheckStore;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * T034 (FR-027 (b), Q-43; module-interfaces mục 2): loại yêu cầu {@code M03.NHAP_BU_PHAN_BO}. Người có quyền chuyển giường
 * lập khi nhập bù lùi quá CFG-M03-07; chỉ Quản lý viện có {@code BED_ALLOCATION} APPROVE duyệt. Khi áp dụng kiểm lại FR-018,
 * FR-019, FR-027 (c) rồi chuyển giường nhập bù tại {@code transferAt}. Nội dung
 * {@code {currentAllocationId, newBedId, transferReasonId, transferAt}}; lý do là lý do của yêu cầu.
 */
@Component
class BackfillRequestHandler implements RequestTypeHandler {
    static final String TYPE = "M03.NHAP_BU_PHAN_BO";

    private record Content(long allocationId, long newBedId, long reasonId, Instant transferAt) {}

    private final BedTransferService transfers;
    private final AllocationCommandStore allocations;
    private final AllocationCheckStore beds;

    BackfillRequestHandler(
            @Lazy BedTransferService transfers, AllocationCommandStore allocations, AllocationCheckStore beds) {
        this.transfers = transfers;
        this.allocations = allocations;
        this.beds = beds;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        read(content);
    }

    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE);
    }

    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.BED_ALLOCATION, Operation.APPROVE);
    }

    @Override
    public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return Optional.of(Set.of(Role.NURSING_HOME_MANAGER));
    }

    @Override
    public Target scope(JsonNode content) {
        var c = read(content);
        return new Target(
                "M03.BED_ALLOCATION",
                Long.toString(c.allocationId()),
                allocations.get(c.allocationId()).residentId(),
                beds.candidate(c.newBedId()).floorId());
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        var c = read(request.content());
        return transfers.backfillConditions(c.allocationId(), c.newBedId(), c.transferAt());
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        var c = read(request.content());
        long next = transfers.applyBackfill(
                c.allocationId(),
                c.newBedId(),
                c.reasonId(),
                new Reason(request.reason()),
                c.transferAt(),
                request.requesterId());
        return new ResultRef("M03.BED_ALLOCATION", Long.toString(next));
    }

    private static Content read(JsonNode content) {
        if (content == null
                || !content.path("currentAllocationId").canConvertToLong()
                || !content.path("newBedId").canConvertToLong()
                || !content.path("transferReasonId").canConvertToLong()
                || !content.path("transferAt").isString())
            throw new InvalidValueException("Nội dung cần currentAllocationId, newBedId, transferReasonId, transferAt");
        try {
            return new Content(
                    content.get("currentAllocationId").asLong(),
                    content.get("newBedId").asLong(),
                    content.get("transferReasonId").asLong(),
                    Instant.parse(content.get("transferAt").asString()));
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("transferAt không hợp lệ");
        }
    }
}
