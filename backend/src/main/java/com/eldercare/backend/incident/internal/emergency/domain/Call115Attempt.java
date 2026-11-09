package com.eldercare.backend.incident.internal.emergency.domain;

import java.time.Instant;

/** Một lượt gọi 115 trả cho client (schema {@code Call115Attempt}); {@code pressedBy} là họ tên người bấm. */
public record Call115Attempt(long id, Instant pressedAt, String pressedBy, String result) {}
