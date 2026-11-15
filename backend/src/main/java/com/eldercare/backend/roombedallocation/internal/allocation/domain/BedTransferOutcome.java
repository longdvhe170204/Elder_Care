package com.eldercare.backend.roombedallocation.internal.allocation.domain;

/** T036: lệnh chuyển giường đã chuyển ngay (200) hoặc đã lập yêu cầu nhập bù chờ duyệt (202, FR-027 (b)). */
public sealed interface BedTransferOutcome {
    record Moved(long newAllocationId) implements BedTransferOutcome {}

    record BackfillRequested(long approvalRequestId, String status) implements BedTransferOutcome {}
}
