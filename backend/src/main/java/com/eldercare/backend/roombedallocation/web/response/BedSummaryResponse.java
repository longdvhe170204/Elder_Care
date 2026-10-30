package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import java.util.Map;

/** Các trường BedSummary trong hợp đồng 003. */
public record BedSummaryResponse(Map<BedStatus, Integer> byState, int usable, int emptyMatching) {}
