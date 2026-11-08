package com.eldercare.backend.nutrition.internal.serving.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.nutrition.internal.serving.service.ServingService.Command;
import com.eldercare.backend.nutrition.internal.serving.service.ServingService.IncidentData;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Xác nhận phục vụ ghi ngoại tuyến (FR-051, Q-01; quickstart Q41): đồng bộ kiểm lại xung đột tại lúc đồng bộ, thời điểm
 * thiết bị lưu riêng.
 */
@Component
public class ServingOfflineHandler implements OfflineHandler<ServingOfflineHandler.Content> {

    public static final String RECORD_TYPE = "M08.SERVING_CONFIRMATION";

    /** Nội dung bản ghi ngoại tuyến: như lệnh {@code confirm-serving}. */
    public record Content(long specialPortionId, long residentId, Boolean hasEaten, IncidentData incident) {}

    private final ServingService serving;

    ServingOfflineHandler(ServingService serving) {
        this.serving = serving;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.SPECIAL_MEAL_SERVICE, Operation.CREATE);
    }

    @Override
    public Target target(Content content) {
        return serving.targetOf(content.specialPortionId());
    }

    @Override
    public void validate(Content content) {
        if (content.specialPortionId() <= 0 || content.residentId() <= 0) {
            throw new InvalidValueException("Thiếu suất hoặc người được phục vụ");
        }
    }

    @Override
    public String apply(Content content, SyncOutcome outcome, Instant effectiveAt) {
        long id = serving.applySynced(
                content.specialPortionId(),
                new Command(content.residentId(), content.hasEaten(), content.incident(), effectiveAt),
                effectiveAt);
        return "serving:" + id;
    }
}
