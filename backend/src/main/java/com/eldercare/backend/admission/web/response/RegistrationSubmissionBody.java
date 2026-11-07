package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.registration.domain.RegistrationSubmission;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionChannel;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Một lần nộp khai báo (contracts/api-018.yaml, schema {@code RegistrationSubmission}; FR-003). */
@Schema(name = "RegistrationSubmission")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegistrationSubmissionBody(
        long id,
        LocalDate submittedDate,
        SubmissionChannel channel,
        String receivingAuthority,
        String dossierCode,
        SubmissionResult result,
        String rejectionReason,
        Instant recordedAt) {

    public static RegistrationSubmissionBody of(RegistrationSubmission submission) {
        return new RegistrationSubmissionBody(
                submission.id(),
                submission.submittedDate(),
                submission.channel(),
                submission.receivingAuthority(),
                submission.dossierCode(),
                submission.result(),
                submission.rejectionReason(),
                submission.recordedAt());
    }
}
