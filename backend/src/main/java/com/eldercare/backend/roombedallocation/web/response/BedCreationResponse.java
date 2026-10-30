package com.eldercare.backend.roombedallocation.web.response;

import java.util.List;

/** T017: tạo giường trả 201 kể cả khi có cảnh báo vượt chuẩn. */
public record BedCreationResponse(BedResponse bed, List<WarningResponse> warnings) {}
