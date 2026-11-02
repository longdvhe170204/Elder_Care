package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** Thân lệnh đính chính (contracts/api-004.yaml, requestBodies {@code CorrectionBody}); chuyển tới 000. */
@Schema(name = "AbsenceCorrectionBody")
public record CorrectionRequestBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Type type,

        Map<String, Object> correctedContent,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {

    public enum Type {
        EDIT_CONTENT(CorrectionType.AMEND_CONTENT),
        CANCEL_RECORD(CorrectionType.VOID);

        private final CorrectionType correctionType;

        Type(CorrectionType correctionType) {
            this.correctionType = correctionType;
        }

        public CorrectionType correctionType() {
            return correctionType;
        }
    }
}
