package com.cambae.backend.m02.internal.registration.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Một lần nộp khai báo (nhóm 3, FR-003; data-model mục 3): chỉ thêm, kết quả đặt một lần. */
public record RegistrationSubmission(
        long id,
        long registrationId,
        LocalDate submittedDate,
        SubmissionChannel channel,
        String receivingAuthority,
        String dossierCode,
        SubmissionResult result,
        String rejectionReason,
        long recordedBy,
        Instant recordedAt) {}
