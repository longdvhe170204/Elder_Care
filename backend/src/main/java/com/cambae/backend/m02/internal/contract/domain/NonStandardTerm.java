package com.cambae.backend.m02.internal.contract.domain;

/**
 * Một điều khoản khác chuẩn của hợp đồng Nháp (FR-025a): giá khác phiên bản đơn giá, hoặc dòng ghi đè chính sách phí khi
 * vắng, giữ giường khác bảng CFG-M02-05. {@code standardValue} rỗng khi không có giá trị chuẩn để so.
 */
public record NonStandardTerm(Type type, String description, String standardValue, String proposedValue) {

    public enum Type {
        PRICE,
        ABSENCE_POLICY,
        BED_HOLD_POLICY
    }
}
