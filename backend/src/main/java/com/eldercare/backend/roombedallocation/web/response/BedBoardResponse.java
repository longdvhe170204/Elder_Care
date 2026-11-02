package com.eldercare.backend.roombedallocation.web.response;

import java.util.List;

/** T017: bảng giường theo hợp đồng; dữ liệu phân bổ thuộc các story sau Phase 3. */
public record BedBoardResponse(List<BedResponse> beds, List<ResidentSummarySchema> residentsWithoutBed) {}
