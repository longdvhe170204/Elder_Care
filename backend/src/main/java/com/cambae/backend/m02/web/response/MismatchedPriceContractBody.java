package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.servicecatalog.domain.MismatchedContract;

/** Hợp đồng lệch phiên bản đơn giá mới nhất (contracts/api-004.yaml, GET {@code /services/{id}/mismatched-price-contracts}; FR-036). */
public record MismatchedPriceContractBody(
        long contractId, String contractNumber, long recordedUnitPrice, long newUnitPrice) {

    public static MismatchedPriceContractBody of(MismatchedContract contract) {
        return new MismatchedPriceContractBody(
                contract.contractId(),
                contract.contractNumber(),
                contract.recordedUnitPrice().longValueExact(),
                contract.newUnitPrice().longValueExact());
    }
}
