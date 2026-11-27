package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Q-453: số việc Nháp đã công bố. */
@Schema(name = "CareMonthPublishResult")
public record CareMonthPublishResponse(int published) {}
