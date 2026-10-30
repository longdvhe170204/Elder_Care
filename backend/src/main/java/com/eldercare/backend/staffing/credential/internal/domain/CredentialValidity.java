package com.eldercare.backend.staffing.credential.internal.domain;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;

/** FR-007, DBR-25: hàm thuần, không đọc đồng hồ hoặc cơ sở dữ liệu; ngày theo múi giờ nghiệp vụ. */
public final class CredentialValidity {
    private CredentialValidity() {}

    public enum Status {
        VALID,
        EXPIRED,
        REVOKED,
        REPLACED,
        NOT_YET_ISSUED
    }

    public static Status status(
            LocalDate issueDate,
            LocalDate expiryDate,
            LocalDate revokedFrom,
            boolean replaced,
            Instant at,
            BusinessTime time) {
        if (replaced) return Status.REPLACED;
        if (revokedFrom != null && !at.isBefore(time.startOfDay(revokedFrom))) return Status.REVOKED;
        if (at.isBefore(time.startOfDay(issueDate))) return Status.NOT_YET_ISSUED;
        if (expiryDate != null && !at.isBefore(time.startOfDay(expiryDate.plusDays(1)))) return Status.EXPIRED;
        return Status.VALID;
    }

    public static boolean covers(
            LocalDate issueDate,
            LocalDate expiryDate,
            LocalDate revokedFrom,
            boolean replaced,
            Instant from,
            Instant to,
            BusinessTime time) {
        return !to.isBefore(from)
                && status(issueDate, expiryDate, revokedFrom, replaced, from, time) == Status.VALID
                && status(issueDate, expiryDate, revokedFrom, replaced, to, time) == Status.VALID;
    }
}
