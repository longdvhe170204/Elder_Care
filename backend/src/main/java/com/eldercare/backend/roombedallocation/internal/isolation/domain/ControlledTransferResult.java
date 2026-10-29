package com.eldercare.backend.roombedallocation.internal.isolation.domain;

import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationDetails;

/** Hai ảnh kết quả sau cùng transaction chuyển giường. */
public record ControlledTransferResult(AllocationDetails previousAllocation, AllocationDetails newAllocation) {}
