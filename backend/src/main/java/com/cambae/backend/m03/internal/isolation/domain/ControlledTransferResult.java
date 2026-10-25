package com.cambae.backend.m03.internal.isolation.domain;

import com.cambae.backend.m03.internal.allocation.domain.AllocationDetails;

/** Hai ảnh kết quả sau cùng transaction chuyển giường. */
public record ControlledTransferResult(AllocationDetails previousAllocation, AllocationDetails newAllocation) {}
