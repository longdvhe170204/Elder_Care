package com.cambae.backend.m02.internal.absence.domain;

/** Loại vắng của lượt vắng (FR-048; glossary 10.3). {@link #HOSPITAL} là Điều trị tại bệnh viện, không phải Tạm vắng. */
public enum AbsenceType {
    HOME_VISIT,
    FAMILY_OUTING,
    SAME_DAY_MEDICAL_VISIT,
    OTHER,
    HOSPITAL;

    /** Bốn loại Tạm vắng, có người đón và bản ghi đón (FR-048). */
    public boolean isTemporaryAbsence() {
        return this != HOSPITAL;
    }
}
