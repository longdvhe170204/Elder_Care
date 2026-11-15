package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/**
 * 014 FR-071 dòng "Người đại diện rút đồng ý về muộn, đăng ký của người bán trú bị hủy; giờ về dự kiến của bán trú được
 * dời theo đồng ý" (FR-016a): người đã đăng ký cho người cao tuổi, Hành chính; người đại diện với lần dời giờ về; Nhẹ.
 * Về việc của chính người thân, không mã Q-354 (009 FR-001a). Nội dung chỉ có buổi, ngày, giờ về dự kiến.
 */
public record LateReturnConsentNotice(
        long sessionId,
        long residentId,
        String change,
        LocalDate date,
        LocalTime expectedReturnTime,
        CareEventContext context)
        implements CareNotice {
    public static final String WITHDRAWN = "WITHDRAWN";
    public static final String RETURN_TIME_CHANGED = "RETURN_TIME_CHANGED";

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        var params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("date", date.toString());
        if (expectedReturnTime != null) params.put("expectedReturnTime", expectedReturnTime.toString());
        return List.of(CareEventNotifications.create(
                this,
                level(),
                context.recipients(),
                eventKey(),
                Map.copyOf(params),
                InformationSubject.ABOUT_FAMILY_MATTER));
    }
}
