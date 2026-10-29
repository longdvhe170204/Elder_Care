package com.eldercare.backend.roombedallocation.internal.bed.domain;

import java.util.List;

/** Kết quả tạo giường và cảnh báo, chưa phải phản hồi HTTP. */
public record BedCreation(Bed bed, List<BedWarning> warnings) {
    public BedCreation {
        warnings = List.copyOf(warnings);
    }
}
