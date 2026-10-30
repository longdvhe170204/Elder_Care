package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** Thân lệnh đính chính (contracts/api-001.yaml, requestBodies {@code CorrectionBody}); chuyển tới 000. */
@Schema(name = "CorrectionBody")
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
