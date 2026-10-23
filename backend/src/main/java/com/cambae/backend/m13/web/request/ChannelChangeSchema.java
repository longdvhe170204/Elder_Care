package com.cambae.backend.m13.web.request;

import com.cambae.backend.m13.web.response.NotificationChannelBody;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Chỉ để mô tả thân {@code PUT /notification-channels/{code}} trong OpenAPI sinh ra như hợp đồng: {@code allOf} gồm
 * {@code NotificationChannel} và phần lý do bắt buộc; thân thật đọc vào {@link ChannelChangeBody}.
 */
@Schema(allOf = {NotificationChannelBody.class, ChannelChangeSchema.ReasonPart.class})
public final class ChannelChangeSchema {

    private ChannelChangeSchema() {}

    @Schema(requiredProperties = {"reason"})
    public record ReasonPart(@Schema(minLength = 1) String reason) {}
}
