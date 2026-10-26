package com.cambae.backend.m02.internal.absence.domain;

import java.time.Instant;

/** Một lần đổi mốc dự kiến trở lại của lượt vắng, kèm lý do (nhóm 3, FR-049). */
public record ExpectedReturnChange(Instant oldValue, Instant newValue, String reason, long actorId, Instant at) {}
