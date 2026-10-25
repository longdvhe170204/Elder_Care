package com.cambae.backend.m03.api.dto;

import java.util.Set;

/** Dấu WITHOUT_BED có bedId null; thay đổi này đã được chủ dự án duyệt tại T061. */
public record BedFlagView(Long residentId, Long bedId, Set<String> flags) {}
