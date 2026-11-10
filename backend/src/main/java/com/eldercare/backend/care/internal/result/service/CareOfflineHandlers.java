package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.result.domain.CareOfflineContents;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.common.offline.SyncResult;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 005 T037 (C-14, BR-M04-12, BR-M04-27, FR-035, FR-036; Q-01): hai {@link OfflineHandler} của 005. Khóa quyền
 * {@code TASK_CHECKLIST} (ghi nhận: CONFIRM như lệnh trực tuyến; Ghi nhận phát sinh: CREATE); áp dụng gọi cùng lệnh với
 * trực tuyến, thời điểm thiết bị là "thời điểm ghi"; {@code ACCEPTED_WITH_FLAG} lưu cờ mất quyền. Công việc đã đóng
 * hoặc Hủy: từ chối kèm người, lúc đóng (002 trả REJECTED, app giữ nội dung để chuyển thành đính chính hoặc Ghi nhận
 * phát sinh). Công việc bị hệ thống đóng vì trạng thái cuối: ghi thành Ghi nhận phát sinh trỏ công việc gốc nếu thực hiện
 * trước lúc đó (CHK056).
 */
@Configuration
public class CareOfflineHandlers {
    static final String TASK_RESULT = "M04.CARE_TASK_RESULT";
    static final String AD_HOC = "M04.AD_HOC_RECORDING";
    private static final String FINAL_RULE = "FR_010_FINAL_STATE";

    @Bean
    OfflineHandler<CareOfflineContents.TaskResult> careTaskResultOfflineHandler(
            CareTaskStore tasks, CareResultService results, AdHocCareRecordingService adHoc) {
        return new OfflineHandler<>() {
            @Override
            public String recordType() {
                return TASK_RESULT;
            }

            @Override
            public PermissionKey permissionKey() {
                return new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM);
            }

            @Override
            public Target target(CareOfflineContents.TaskResult content) {
                return CareTaskService.target(tasks.get(content.taskId(), false));
            }

            @Override
            public void validate(CareOfflineContents.TaskResult content) {
                if (content.taskId() == null) throw new InvalidValueException("Cần taskId");
                switch (content.action() == null ? "" : content.action()) {
                    case "RECORD" -> {
                        if (content.recording() == null) throw new InvalidValueException("RECORD cần recording");
                    }
                    case "NOT_PERFORM" -> {
                        if (content.reasonTypeId() == null)
                            throw new InvalidValueException("NOT_PERFORM cần reasonTypeId");
                    }
                    default -> throw new InvalidValueException("action là RECORD hoặc NOT_PERFORM");
                }
            }

            @Override
            public String apply(CareOfflineContents.TaskResult content, SyncOutcome outcome, Instant deviceAt) {
                long id = content.taskId();
                boolean lost = outcome.result() == SyncResult.ACCEPTED_WITH_FLAG;
                var task = tasks.get(id, false);
                var closure = tasks.closure(id);
                boolean finalState = task.status() == TaskStatus.NOT_PERFORMED && FINAL_RULE.equals(closure.rule());
                if (finalState && "RECORD".equals(content.action()) && task.residentId() != null) {
                    var saved = adHoc.recordOffline(
                            task.residentId(),
                            task.taskTypeId(),
                            content.recording().input(),
                            true,
                            id,
                            deviceAt,
                            lost);
                    return Long.toString(saved.taskId());
                }
                try {
                    if ("RECORD".equals(content.action()))
                        results.recordOffline(id, content.recording().input(), deviceAt, lost);
                    else results.notPerformedOffline(id, content.reasonTypeId(), content.note(), deviceAt);
                } catch (BusinessException e) {
                    if (!"CARE_TASK_CLOSED".equals(e.errorCode())) throw e;
                    throw closed(e);
                }
                return Long.toString(id);
            }
        };
    }

    @Bean
    OfflineHandler<CareOfflineContents.AdHocRecording> adHocRecordingOfflineHandler(AdHocCareRecordingService adHoc) {
        return new OfflineHandler<>() {
            @Override
            public String recordType() {
                return AD_HOC;
            }

            @Override
            public PermissionKey permissionKey() {
                return new PermissionKey(Function.TASK_CHECKLIST, Operation.CREATE);
            }

            @Override
            public Target target(CareOfflineContents.AdHocRecording content) {
                return adHoc.target(content.residentId(), content.performedAt());
            }

            @Override
            public void validate(CareOfflineContents.AdHocRecording content) {
                if (content.residentId() == null || content.taskTypeId() == null)
                    throw new InvalidValueException("Cần người cao tuổi và loại công việc");
            }

            @Override
            public String apply(CareOfflineContents.AdHocRecording content, SyncOutcome outcome, Instant deviceAt) {
                var saved = adHoc.recordOffline(
                        content.residentId(),
                        content.taskTypeId(),
                        content.input(),
                        Boolean.TRUE.equals(content.sameTypeTasksReviewed()),
                        content.originalTaskId(),
                        deviceAt,
                        outcome.result() == SyncResult.ACCEPTED_WITH_FLAG);
                if (saved.taskId() == null)
                    throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "SAME_TYPE_TASKS_OPEN",
                            "SAME_TYPE_TASKS_OPEN: có công việc cùng loại đang mở, cần xem lại trước khi ghi");
                return Long.toString(saved.taskId());
            }
        };
    }

    /** FR-036: báo ai đã đóng công việc và lúc nào; nội dung giữ trên máy. */
    private static BusinessException closed(BusinessException e) {
        var p = e.properties();
        return new BusinessException(
                HttpStatus.CONFLICT,
                "CARE_TASK_CLOSED",
                "CARE_TASK_CLOSED: công việc " + p.getOrDefault("currentStatus", "đã đóng") + ", đóng bởi "
                        + p.getOrDefault("closedBy", "hệ thống") + " lúc " + p.getOrDefault("closedAt", "?")
                        + "; chuyển thành đính chính hoặc Ghi nhận phát sinh",
                p);
    }
}
