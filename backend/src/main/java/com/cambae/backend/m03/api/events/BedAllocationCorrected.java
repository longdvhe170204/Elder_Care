package com.cambae.backend.m03.api.events;

import java.util.Map;

/** BedAllocationCorrected: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record BedAllocationCorrected(long allocationId, Map<String, Object> before, Map<String, Object> after) {}
