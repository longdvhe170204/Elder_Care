package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.administration.correction.api.CorrectionType;
import jakarta.validation.constraints.*;
import java.util.Map;

/** Request đính chính Phase 7 đã được duyệt dịch, không thay hợp đồng 000. */
public record RecordCorrection(
        @NotNull CorrectionType type,
        Map<String, Object> correctedContent,
        @NotBlank @Size(max = 2000) String reason) {}
