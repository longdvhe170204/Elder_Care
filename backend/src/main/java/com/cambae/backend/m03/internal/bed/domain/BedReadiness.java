package com.cambae.backend.m03.internal.bed.domain;

import com.cambae.backend.common.error.UnmetCondition;
import java.util.List;

/** Điều kiện đọc tại thời điểm hỏi; lệnh xác nhận luôn kiểm lại nguồn. */
public record BedReadiness(
        boolean allowed, boolean disinfectionRequired, Long departedResidentId, List<UnmetCondition> unmetConditions) {}
