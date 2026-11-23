package com.eldercare.backend.admission.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Thân lệnh trên khai báo (contracts/api-018.yaml, schema {@code RegistrationCommand}): {@code basisNote} (US1); ảnh bằng
 * chứng (cả bằng chứng khai báo xóa, US4), ngày hết hạn (US2, Q-414).
 */
@Schema(name = "RegistrationCommand")
public record RegistrationCommandBody(String basisNote, Long evidenceFileId, LocalDate expiryDate) {}
