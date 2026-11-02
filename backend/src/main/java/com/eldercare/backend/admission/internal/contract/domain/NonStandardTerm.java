package com.eldercare.backend.admission.internal.contract.domain;

/** Một điều khoản khác chuẩn của hợp đồng Nháp (FR-025a; Q-395 bỏ ghi đè chính sách vắng). {@code standardValue} rỗng khi không có giá trị chuẩn để so. */
public record NonStandardTerm(Type type, String description, String standardValue, String proposedValue) {

    public enum Type {
        PRICE
    }
}
