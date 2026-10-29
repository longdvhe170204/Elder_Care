package com.eldercare.backend.notification.internal.quiethours.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.administration.parameter.api.ParameterKind;
import com.eldercare.backend.administration.parameter.api.ParameterValue;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.quiethours.domain.QuietHours;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Áp giờ yên tĩnh CFG-M13-02 (FR-022, FR-023, research B-06): chỉ thông báo Nhẹ, Trung bình cho người thân và người cao
 * tuổi (nhóm (f)); Khẩn cấp và nhân viên không bao giờ giữ.
 */
@Service
public class QuietHoursPolicy {

    /** Giá trị mặc định của CFG-M13-02 (FR-022) khi viện chưa đặt giá trị. */
    static final QuietHours DEFAULT = new QuietHours(LocalTime.of(21, 0), LocalTime.of(7, 0));

    private final ParameterApi parameters;

    QuietHoursPolicy(ParameterApi parameters) {
        this.parameters = parameters;
    }

    /** Giá trị hiện hành; đọc một lần cho mỗi lần xét (FR-018 của 000). */
    public QuietHours current() {
        return of(parameters.read(ParameterCode.CFG_M13_02));
    }

    /** Giá trị mang theo sự kiện đổi tham số ({@code ParameterChanged.after}). */
    public static QuietHours of(JsonNode raw) {
        return of(new ParameterValue(ParameterCode.CFG_M13_02, ParameterKind.TABLE, raw));
    }

    private static QuietHours of(ParameterValue v) {
        return v.isSet() ? QuietHours.of(v.asTable(QuietHours.Value.class)) : DEFAULT;
    }

    public static boolean applies(RecipientType type, NotificationLevel level) {
        return level != NotificationLevel.URGENT && type != RecipientType.STAFF;
    }

    /** Thời điểm gửi dự kiến khi phải giữ thông báo gửi lúc {@code at}. */
    public static Optional<Instant> holdUntil(
            QuietHours quiet, RecipientType type, NotificationLevel level, Instant at) {
        return applies(type, level) ? quiet.endIfWithin(at) : Optional.empty();
    }
}
