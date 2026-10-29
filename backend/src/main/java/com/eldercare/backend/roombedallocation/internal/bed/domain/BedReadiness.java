package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.common.error.UnmetCondition;
import java.util.List;

/** Điều kiện đọc tại thời điểm hỏi; lệnh xác nhận luôn kiểm lại nguồn. */
public record BedReadiness(
        boolean allowed, boolean disinfectionRequired, Long departedResidentId, List<UnmetCondition> unmetConditions) {}
