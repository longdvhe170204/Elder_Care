package com.cambae.backend.m11.api;

import com.cambae.backend.m11.api.dto.*;
import com.cambae.backend.m15.account.api.CurrentUser;
import java.time.LocalDate;
import java.util.List;

/** FR-037/038, FR-023a: bản cài kiểm quyền người gọi và che tên thuốc theo FR-019. */
public interface FamilyPortalCostApi {
    List<CostStatementView> finalizedStatements(long residentId, CurrentUser familyMember);

    ProvisionalCostSummaryView provisional(long residentId, LocalDate from, LocalDate to, CurrentUser user);

    List<PurchaseAssistanceView> awaitingConsent(long residentId, long familyMemberId);
}
