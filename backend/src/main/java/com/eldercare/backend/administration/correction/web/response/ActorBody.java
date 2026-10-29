package com.eldercare.backend.administration.correction.web.response;

/** Người thực hiện theo schema {@code Actor}; họ tên theo hồ sơ tài khoản (002 T032a). */
public record ActorBody(ActorType type, Long id, String fullName, String role) {}
