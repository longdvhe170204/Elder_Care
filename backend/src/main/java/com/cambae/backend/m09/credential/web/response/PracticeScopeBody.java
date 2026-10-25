package com.cambae.backend.m09.credential.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Mã lựa chọn trên HTTP 008; không đổi PracticeScopeView của SPI 002. */
@Schema(name = "StaffPracticeScope")
public record PracticeScopeBody(long id, String name, boolean active, String code) {}
