package com.eldercare.backend.care.internal.result.domain;

import com.eldercare.backend.care.internal.task.domain.CareTask;
import java.util.List;

/** Response 200 chưa ghi hoặc 201 đã ghi, không tự chọn công việc thay cho người dùng. */
public record AdHocCareRecording(Long taskId, List<CareTask> sameTypeTasks) {}
