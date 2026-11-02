package com.eldercare.backend.billing.api;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.billing.api.dto.*;
import java.time.LocalDate;
import java.util.List;

/** FR-037/038, FR-023a: bản cài kiểm quyền người gọi và che tên thuốc theo FR-019. */
public interface FamilyPortalCostApi {
    List<CostStatementView> finalizedStatements(long residentId, CurrentUser familyMember);

    ProvisionalCostSummaryView provisional(long residentId, LocalDate from, LocalDate to, CurrentUser user);

    List<PurchaseAssistanceView> awaitingConsent(long residentId, long familyMemberId);
}
