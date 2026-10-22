package com.cambae.backend.common.document;

/**
 * Thông tin viện in ở đầu chứng từ: giá trị của CFG-M11-11 (kiểu bảng), module gọi đọc bằng
 * {@code ParameterApi.read(CFG_M11_11).asTable(FacilityInfo.class)} (007 research S-25). Trường chưa nhập thì
 * {@code null}, không in; {@code logoFileId} là id tệp {@code common.file}.
 */
public record FacilityInfo(String name, String address, String taxCode, String phone, Long logoFileId) {}
