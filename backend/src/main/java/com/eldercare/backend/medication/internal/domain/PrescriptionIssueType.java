package com.eldercare.backend.medication.internal.domain;

/**
 * Issue type detected during new prescription validation (FR-007, FR-008, BR-M07-06).
 */
public enum PrescriptionIssueType {
    /** Duplicate active ingredient with existing active prescription */
    DUPLICATE_ACTIVE_INGREDIENT,
    /** Conflict with allergy in resident health record */
    ALLERGY_CONFLICT,
    /** Free-text 'other' allergy item requiring manual review */
    OTHER_ALLERGY_REVIEW
}
