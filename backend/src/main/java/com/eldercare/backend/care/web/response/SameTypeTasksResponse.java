package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Ghi nhận phát sinh khi còn công việc cùng loại Đến hạn, Quá hạn chưa xem lại (api-005, phản hồi 200). */
@Schema(name = "CareSameTypeTasks")
public record SameTypeTasksResponse(List<CareTaskResponse> sameTypeTasks) {}
