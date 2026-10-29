package com.eldercare.backend.roombedallocation.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Không trả thông tin quản trị tài khoản cho người chọn Bác sĩ. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrescribingDoctorResponse(long id, String fullName) {}
