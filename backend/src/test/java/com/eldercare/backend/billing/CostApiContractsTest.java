package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.*;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.api.events.*;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseBalancePort;
import com.eldercare.backend.common.notification.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/** T003/T004: hợp đồng cho module gọi và khả năng đọc lại sự kiện outbox. */
class CostApiContractsTest {
    private static final Instant AT = Instant.parse("2026-10-29T10:00:00Z");

    @Test
    void FR_004_costItemChangedReadsLegacyOutboxAndKeepsItsOldConstructor() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var legacy = mapper.readValue("{\"residentId\":23,\"periodId\":19}", CostItemChanged.class);
        assertThat(legacy).isEqualTo(new CostItemChanged(23, 19));
        assertThat(legacy.notificationRequests()).isEmpty();
        var notice = notificationEvents()
                .findFirst()
                .orElseThrow()
                .notificationRequests()
                .getFirst();
        var current = new CostItemChanged(23, 19, List.of(notice));
        assertThat(mapper.readValue(mapper.writeValueAsString(current), CostItemChanged.class))
                .isEqualTo(current);
    }

    @ParameterizedTest
    @CsvSource({"FAMILY_MEAL, true", "OVERNIGHT_STAY, false", "ACTIVITY_ATTENDANCE, true"})
    void FR_005_sourceEventRetainsSourceIdentityQuantityAndOptionalService(String type, boolean pricedService) {
        var original = new CostSourceBilled(
                type,
                "17",
                "UNIT",
                23,
                pricedService ? Optional.of(41L) : Optional.empty(),
                new BigDecimal("1.25"),
                LocalDate.of(2026, 10, 29),
                AT,
                "source:17:1");
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var payload = mapper.writeValueAsString(original);
        assertThat(payload.length()).isLessThanOrEqualTo(4000);
        assertThat(mapper.readValue(payload, CostSourceBilled.class)).isEqualTo(original);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void FR_020_sourceCancellationRemainsDifferentFromAZeroQuantity(boolean cancelled) {
        var original = new CostSourceChanged(
                "FAMILY_MEAL",
                "17",
                "UNIT",
                cancelled ? Optional.empty() : Optional.of(BigDecimal.ZERO),
                cancelled,
                "Đính chính nguồn",
                "change:17:2");
        var mapper = JsonMapper.builder().findAndAddModules().build();
        assertThat(mapper.readValue(mapper.writeValueAsString(original), CostSourceChanged.class))
                .isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("notificationEvents")
    void FR_044_familyNotificationMetadataSurvivesOutboxRestart(HasNotificationRequests original) {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var payload = mapper.writeValueAsString(original);
        assertThat(payload.length()).isLessThanOrEqualTo(4000);
        var restored = (HasNotificationRequests) mapper.readValue(payload, original.getClass());
        assertThat(restored).isEqualTo(original);
        assertThat(restored.notificationRequests()).allSatisfy(request -> {
            assertThat(request.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER);
            assertThat(request.q354Case()).isEmpty();
            assertThat(request.content())
                    .allSatisfy(part -> assertThat(part.type()).isEqualTo(InformationType.COSTS));
        });
    }

    static Stream<HasNotificationRequests> notificationEvents() {
        var request = new NotificationRequest(
                "010",
                "COST_CONTRACT_TEST",
                SourceGroup.OTHER,
                new SourceRecord("M11.COST_STATEMENT", "17"),
                "cost:test:17",
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(23L),
                Optional.empty(),
                List.of(new RecipientGroup.FamilyWithRight(23, FamilyRight.VIEW_COSTS)),
                List.of(new ContentPart(InformationType.COSTS, "Có thông tin chi phí cần xem")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                AT,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty());
        var notices = List.of(request);
        return Stream.of(
                new CostStatementFinalized(17, CostStatementType.REGULAR, 19, 23, -500L, AT, notices),
                new PurchaseAssistanceSent(17, 23, 500L, PurchaseAssistanceStatus.AWAITING_CONSENT, notices),
                new PurchaseAssistanceDecided(17, 23, 500L, PurchaseAssistanceStatus.PURCHASE_ALLOWED, notices),
                new PurchaseAssistanceCancelled(17, 23, 500L, PurchaseAssistanceStatus.CANCELLED, notices),
                new PurchaseAssistancePurchased(17, 23, 500L, PurchaseAssistanceStatus.PURCHASED, notices));
    }

    @Test
    void FR_037_portalContractCarriesTheCallerAndDateRange() throws Exception {
        assertThat(FamilyPortalCostApi.class
                        .getMethod("finalizedStatements", long.class, CurrentUser.class)
                        .getGenericReturnType()
                        .getTypeName())
                .contains("CostStatementView");
        assertThat(FamilyPortalCostApi.class
                        .getMethod("provisional", long.class, LocalDate.class, LocalDate.class, CurrentUser.class)
                        .getReturnType())
                .isEqualTo(ProvisionalCostSummaryView.class);
        assertThat(FamilyPortalCostApi.class
                        .getMethod("awaitingConsent", long.class, long.class)
                        .getGenericReturnType()
                        .getTypeName())
                .contains("PurchaseAssistanceView");
        assertThat(FinalPeriodCostApi.class
                        .getMethod("finalPeriodCondition", long.class)
                        .getReturnType())
                .isEqualTo(FinalPeriodConditionView.class);
    }

    @Test
    void FR_042_statisticsContractIncludesScopeDatesAndOptionalPeriod() throws Exception {
        assertThat(CostStatisticsApi.class
                        .getMethod("items", ScopeFilter.class, LocalDate.class, LocalDate.class)
                        .getGenericReturnType()
                        .getTypeName())
                .contains("Stream", "CostItemReportView");
        assertThat(CostStatisticsApi.class
                        .getMethod("statements", Optional.class)
                        .getGenericReturnType()
                        .getTypeName())
                .contains("Stream", "CostStatementReportView");
        assertThat(CostStatisticsApi.class
                        .getMethod("periods")
                        .getGenericReturnType()
                        .getTypeName())
                .contains("Stream", "CostPeriodView");
    }

    @Test
    void FR_038a_balancePortsKeepSignedDongAndTheRequestedInstant() throws Exception {
        assertThat(CostForBalancePort.class
                        .getMethod("unfinalizedTotal", long.class)
                        .getReturnType())
                .isEqualTo(long.class);
        assertThat(CostForBalancePort.class
                        .getMethod("baseDailyFee", long.class, LocalDate.class)
                        .getGenericReturnType()
                        .getTypeName())
                .contains("Optional", "Long");
        assertThat(PurchaseBalancePort.class
                        .getMethod("insufficientBalance", long.class, long.class, Instant.class)
                        .getReturnType())
                .isEqualTo(boolean.class);
        assertThat(CostForBalancePort.class.getPackageName()).endsWith(".internal.provision.domain");
        assertThat(PurchaseBalancePort.class.getPackageName()).endsWith(".internal.purchase.domain");
    }
}
