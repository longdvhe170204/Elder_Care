package com.eldercare.backend.admission.internal.registration.domain;

/** Kết quả của một lần nộp (FR-003; data-model mục 3): đặt một lần từ {@code PENDING}. */
public enum SubmissionResult {
    PENDING,
    ACCEPTED,
    REJECTED
}
