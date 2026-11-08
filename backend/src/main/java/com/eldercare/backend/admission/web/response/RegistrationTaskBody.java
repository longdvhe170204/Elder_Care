package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService.Task;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService.TaskType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Việc khai báo cần làm (contracts/api-018.yaml, schema {@code RegistrationTask}; FR-001, FR-002). */
@Schema(name = "RegistrationTask")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegistrationTaskBody(
        long residentId,
        String fullName,
        TaskType taskType,
        Instant dueAt,
        Long registrationId,
        List<String> allowedCommands) {

    public static RegistrationTaskBody of(Task task) {
        return new RegistrationTaskBody(
                task.residentId(),
                task.fullName(),
                task.taskType(),
                task.dueAt(),
                task.registrationId(),
                task.allowedCommands());
    }
}
