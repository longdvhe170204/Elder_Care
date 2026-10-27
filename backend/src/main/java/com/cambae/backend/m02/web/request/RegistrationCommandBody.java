package com.cambae.backend.m02.web.request;

import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Thân lệnh trên khai báo (contracts/api-018.yaml, schema {@code RegistrationCommand}). Phase 3 (US1) dùng {@code
 * basisNote}; trường ghi nộp, kết quả, khai báo xóa dùng từ Phase 4, 6.
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
