package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Q-453: kết quả "Chép từ kế hoạch": số việc Nháp tạo, số mốc bỏ qua (đã có hoặc không đạt kiểm tra), số ca đã duyệt. */
@Schema(name = "CareMonthCopyResult")
public record CareMonthCopyResponse(int created, int skipped, int shifts) {}
