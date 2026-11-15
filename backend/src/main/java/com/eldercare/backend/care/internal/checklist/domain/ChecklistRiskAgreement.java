package com.eldercare.backend.care.internal.checklist.domain;

import com.eldercare.backend.care.internal.careplan.domain.CareRiskAgreement;

/** Thỏa thuận rủi ro đang hiệu lực của người cao tuổi trên checklist ca (FR-030), bản đủ như kế hoạch chăm sóc. */
public record ChecklistRiskAgreement(long residentId, CareRiskAgreement agreement) {}
