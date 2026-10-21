package com.cambae.backend.m01.api.dto;

import com.cambae.backend.m15.account.api.CurrentUser;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Ngữ cảnh một lệnh trạng thái đưa cho module tham gia (contracts mục 1.3). */
public record CommandContext(
        long residentId, ResidentStatus from, StatusCommand command, CurrentUser user, Instant at, JsonNode details) {}
