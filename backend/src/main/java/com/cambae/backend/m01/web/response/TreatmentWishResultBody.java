package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService.Result;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Kết quả ghi phiếu nguyện vọng khi điều trị (contracts/api-001.yaml, schema {@code TreatmentWishResult}). */
@Schema(name = "TreatmentWishResult")
public record TreatmentWishResultBody(TreatmentWishBody wish, List<Warning> warnings) {

    public static TreatmentWishResultBody of(Result result, TreatmentWishService service) {
        return new TreatmentWishResultBody(TreatmentWishBody.of(result.wish(), service), result.warnings());
    }
}
