package com.eldercare.backend.notification.internal.directcontact.domain;

/** Kết quả liên hệ đã ghi, người liên hệ và người cao tuổi của yêu cầu (đính chính, FR-031). */
public record RecordedContactResult(long contactedBy, DirectContactResultContent content, Long residentId) {}
