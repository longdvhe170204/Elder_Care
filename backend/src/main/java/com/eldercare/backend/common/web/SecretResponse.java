package com.eldercare.backend.common.web;

/**
 * Thân phản hồi chứa bí mật chỉ trả một lần (mật khẩu tạm, mã QR đăng nhập): {@link IdempotencyFilter} không lưu nội dung,
 * vì bí mật chỉ được lưu dạng băm (NFR-08, 002 plan).
 */
public interface SecretResponse {}
