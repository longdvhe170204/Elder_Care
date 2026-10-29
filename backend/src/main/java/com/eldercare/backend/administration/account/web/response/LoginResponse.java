package com.eldercare.backend.administration.account.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code token} chỉ với kênh app; {@code deviceToken} chỉ kênh người cao tuổi (US8). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String token, String deviceToken, boolean mustChangePassword, UserInfo user) {}
