package com.eldercare.backend.family.internal.right.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Nhắc yêu cầu Chờ xác nhận lâu theo CFG-M10-11 (FR-019; research F-06), chạy trong việc hằng giờ của M10: quá mốc nhắc
 * thì nhắc người đại diện và Quản lý viện, quá mốc báo thì báo Quản lý viện, mỗi mốc một lần (dấu {@code reminder_marker});
 * không tự Từ chối (Q-307).
 */
@Component
class RightChangeReminders implements M10TimedTask {

    static final String MARKER_KIND = "M10.RIGHT_CHANGE_REMINDER";

    record Settings(Threshold remind, Threshold notifyManager) {}

    record Threshold(long value, String unit) {

        Duration duration() {
            return switch (unit) {
                case "MINUTES" -> Duration.ofMinutes(value);
                case "HOURS" -> Duration.ofHours(value);
                case "DAYS" -> Duration.ofDays(value);
                default -> throw new IllegalStateException("CFG-M10-11 đơn vị " + unit);
            };
        }
    }

    private final SimpleRecordApi records;
    private final ReminderMarkers markers;
    private final ParameterApi parameters;
    private final RightChangeRequestService requests;

    RightChangeReminders(
            SimpleRecordApi records,
            ReminderMarkers markers,
            ParameterApi parameters,
            RightChangeRequestService requests) {
        this.records = records;
        this.markers = markers;
        this.parameters = parameters;
        this.requests = requests;
    }

    @Override
    public String name() {
        return "RIGHT_CHANGE_REMINDER";
    }

    @Override
    public Cadence cadence() {
        return Cadence.HOURLY;
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        Settings settings = parameters.read(ParameterCode.CFG_M10_11).asTable(Settings.class);
        Duration remind = settings.remind().duration();
        Duration notifyManager = settings.notifyManager().duration();
        int done = 0;
        for (SimpleRecord<RightChangeRequest> request : records.listByStatus(
                RightChangeRequest.KIND, Set.of(RightChangeRequest.Status.PENDING_CONFIRMATION.name()))) {
            Duration age = Duration.between(request.createdAt(), actualAt);
            if (age.compareTo(remind) >= 0
                    && markers.mark(MARKER_KIND, RightChangeRequest.KIND.code(), request.id(), "REMIND", null)) {
                requests.remind(request.residentId(), request.id(), request.content(), false, actualAt);
                done++;
            }
            if (age.compareTo(notifyManager) >= 0
                    && markers.mark(
                            MARKER_KIND, RightChangeRequest.KIND.code(), request.id(), "NOTIFY_MANAGER", null)) {
                requests.remind(request.residentId(), request.id(), request.content(), true, actualAt);
                done++;
            }
        }
        return done;
    }
}
