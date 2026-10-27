package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.servicecatalog.domain.PriceVersion;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Phiên bản đơn giá (contracts/api-004.yaml, schema {@code PriceVersion}; FR-035). */
@Schema(name = "PriceVersion")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceVersionBody(
        long id, long unitPrice, LocalDate effectiveFrom, LocalDate effectiveTo, boolean referenced) {

    public static PriceVersionBody of(PriceVersion version) {
        return new PriceVersionBody(
                version.id(),
                version.unitPrice().longValueExact(),
                version.effectiveFrom(),
                version.effectiveTo(),
                version.referenced());
    }
}
