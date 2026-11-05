package com.eldercare.backend.family.internal.right.domain;

import java.time.Instant;

/** Dấu "được tự về" vừa tắt (sự kiện trong M10): bản ghi đón Tự về chưa dùng chuyển Không dùng ngay (FR-017). */
public record SelfReturnTurnedOff(long residentId, Instant at) {}
