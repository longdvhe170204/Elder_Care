package com.cambae.backend.m03.web.response;

import java.util.List;

/** Kết quả chuyển theo schema Phase5 đã dịch, dùng lại cho Phase9. */
public record BedTransferResult(
        BedAllocationResponse previousAllocation,
        BedAllocationResponse newAllocation,
        List<WarningResponse> warnings) {}
