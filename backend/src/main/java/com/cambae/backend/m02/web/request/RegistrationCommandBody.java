package com.cambae.backend.m02.web.request;

import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Thân lệnh trên khai báo (contracts/api-018.yaml, schema {@code RegistrationCommand}): {@code basisNote} (US1); ngày nộp,
 * kênh, cơ quan, mã hồ sơ, bằng chứng (cả bằng chứng khai báo xóa, US4), ngày hết hạn, lý do (US2).
 */
@Schema(name = "RegistrationCommand")
public record RegistrationCommandBody(
        String basisNote,
        LocalDate submittedDate,
        SubmissionChannel channel,
        String receivingAuthority,
        String dossierCode,
        Long evidenceFileId,
        LocalDate expiryDate,
        String reason) {}
