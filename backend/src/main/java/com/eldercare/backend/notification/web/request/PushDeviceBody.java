package com.eldercare.backend.notification.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Thân {@code POST /push-devices}: app đang đăng nhập và token FCM của thiết bị (research B-05). */
public record PushDeviceBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        App app,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String fcmToken) {

    public enum App {
        STAFF_APP,
        FAMILY_APP,
        RESIDENT_APP
    }
}
