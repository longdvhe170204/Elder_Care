package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.internal.structure.domain.BedTransferReason;
import com.fasterxml.jackson.annotation.JsonInclude;

/** T017: phản hồi chỉ gồm trường hợp đồng, không serialize trực tiếp entity JPA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedTransferReasonResponse(
        long id, String code, String name, boolean medicalSafety, boolean fixed, String status) {
    public static BedTransferReasonResponse of(BedTransferReason e) {
        return new BedTransferReasonResponse(
                e.getId(),
                e.code(),
                e.getName(),
                e.medicalSafety(),
                e.fixed(),
                e.getStatus().name());
    }
}
