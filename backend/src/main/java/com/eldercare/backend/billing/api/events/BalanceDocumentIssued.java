package com.eldercare.backend.billing.api.events;

import com.eldercare.backend.billing.api.dto.BalanceDocumentType;

/** Chứng từ vừa phát hành (tên logic {@code ChungTuDaPhatHanh}; FR-031, FR-034, FR-038); 012 nghe. */
public record BalanceDocumentIssued(long residentId, BalanceDocumentType type, long id) {}
