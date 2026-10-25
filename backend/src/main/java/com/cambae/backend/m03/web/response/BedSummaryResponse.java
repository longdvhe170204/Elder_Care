package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.dto.BedStatus;
import java.util.Map;

/** Các trường BedSummary trong hợp đồng 003. */
public record BedSummaryResponse(Map<BedStatus, Integer> byState, int usable, int emptyMatching) {}
