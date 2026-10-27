package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService.Task;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService.TaskType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Việc khai báo cần làm (contracts/api-018.yaml, schema {@code RegistrationTask}; FR-001, FR-002). */
@Schema(name = "RegistrationTask")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegistrationTaskBody(
        long residentId, String fullName, TaskType taskType, Instant dueAt, Long registrationId) {

    public static RegistrationTaskBody of(Task task) {
        return new RegistrationTaskBody(
                task.residentId(), task.fullName(), task.taskType(), task.dueAt(), task.registrationId());
    }
}
