package com.cambae.backend.m09.credential.internal.domain;

import java.time.LocalDate;
import java.util.Set;

/** Bản chứng chỉ và trạng thái thay thế suy ra; không lưu tình trạng hiệu lực. */
public record Credential(
        long id,
        long staffId,
        long credentialTypeId,
        String typeCode,
        String typeName,
        String group,
        String credentialNumber,
        String issuingAuthority,
        Set<String> practiceScopes,
        Set<Long> practiceScopeIds,
        LocalDate issueDate,
        LocalDate expiryDate,
        Long scanFileId,
        Long replacesId,
        LocalDate revokedFrom,
        boolean replaced) {}
