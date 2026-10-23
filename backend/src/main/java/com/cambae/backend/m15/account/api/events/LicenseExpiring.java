package com.cambae.backend.m15.account.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Giấy phép cơ sở hoặc người hành nghề sắp hết hạn; báo Quản lý viện (FR-039; 002 research A-09). */
public record LicenseExpiring(
        String licenseKind,
        long licenseId,
        LocalDate expiryDate,
        List<String> affectedActivities,
        List<Long> relatedStaffIds,
        Instant at)
        implements HasNotificationRequests {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<NotificationRequest> notificationRequests() {
        String kind = "FACILITY".equals(licenseKind) ? "Giấy phép cơ sở" : "Giấy phép hành nghề";
        return List.of(Notices.of(
                "LICENSE_EXPIRING",
                SourceGroup.LICENSE_EXPIRING,
                new SourceRecord("M15.LICENSE_EXPIRY_WARNING", licenseKind + ":" + licenseId),
                "LICENSE_EXPIRING:" + expiryDate,
                null,
                Notices.MANAGERS,
                kind + " sắp hết hạn",
                kind + " hết hạn ngày " + DATE.format(expiryDate)
                        + "; sau ngày này các nghiệp vụ chuyên môn cần giấy phép sẽ bị từ chối.",
                at));
    }
}
