package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.contract.domain.NonStandardTerm;
import io.swagger.v3.oas.annotations.media.Schema;

/** Điều khoản khác chuẩn của hợp đồng Nháp (contracts/api-004.yaml, schema {@code NonStandardTerm}; FR-025a). */
@Schema(name = "NonStandardTerm")
public record NonStandardTermBody(
        NonStandardTerm.Type type, String description, String standardValue, String proposedValue) {

    public static NonStandardTermBody of(NonStandardTerm term) {
        return new NonStandardTermBody(term.type(), term.description(), term.standardValue(), term.proposedValue());
    }
}
