package com.eldercare.backend.care.internal.governance.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.care.internal.governance.domain.CareRetentionType;
import com.eldercare.backend.care.internal.governance.persistence.CareGovernanceStore;
import com.eldercare.backend.care.internal.governance.service.*;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;

/** T008: đăng ký metadata và provider; không tạo các bảng/bản ghi story trước thời hạn. */
@Configuration
public class CareGovernanceConfiguration {
    @Bean
    CareFieldClassification fieldsCARE_PLAN() {
        return new CareFieldClassification(
                "M04.CARE_PLAN",
                Map.ofEntries(
                        Map.entry("content", InfoGroup.HEALTH),
                        Map.entry("items", InfoGroup.HEALTH),
                        Map.entry("riskAgreements", InfoGroup.HEALTH)));
    }

    @Bean
    CareFieldClassification fieldsTASK() {
        return new CareFieldClassification(
                "M04.TASK",
                Map.ofEntries(
                        Map.entry("content", InfoGroup.HEALTH),
                        Map.entry("nonPerformanceReasonTypeId", InfoGroup.HEALTH),
                        Map.entry("nonPerformanceNote", InfoGroup.HEALTH),
                        Map.entry("status", InfoGroup.INTERNAL_PUBLIC),
                        Map.entry("plannedAt", InfoGroup.INTERNAL_PUBLIC),
                        Map.entry("performerId", InfoGroup.INTERNAL_PUBLIC)));
    }

    @Bean
    CareFieldClassification fieldsCARE_RESULT() {
        return new CareFieldClassification(
                "M04.CARE_RESULT",
                Map.ofEntries(
                        Map.entry("values", InfoGroup.HEALTH),
                        Map.entry("note", InfoGroup.HEALTH),
                        Map.entry("resultType", InfoGroup.HEALTH),
                        Map.entry("numericValue", InfoGroup.HEALTH),
                        Map.entry("codedValue", InfoGroup.HEALTH),
                        Map.entry("nonPerformanceReasonTypeId", InfoGroup.HEALTH),
                        Map.entry("nonPerformanceNote", InfoGroup.HEALTH)));
    }

    @Bean
    CareFieldClassification fieldsRISK_AGREEMENT() {
        return new CareFieldClassification(
                "M04.RISK_AGREEMENT",
                Map.ofEntries(
                        Map.entry("id", InfoGroup.HEALTH),
                        Map.entry("carePlanId", InfoGroup.HEALTH),
                        Map.entry("sourceAgreementId", InfoGroup.HEALTH),
                        Map.entry("recordedById", InfoGroup.HEALTH),
                        Map.entry("recordedAt", InfoGroup.HEALTH),
                        Map.entry("choice", InfoGroup.HEALTH),
                        Map.entry("discussedRisks", InfoGroup.HEALTH),
                        Map.entry("residentParticipated", InfoGroup.HEALTH),
                        Map.entry("doctorId", InfoGroup.HEALTH),
                        Map.entry("canDecide", InfoGroup.HEALTH),
                        Map.entry("assessedAt", InfoGroup.HEALTH),
                        Map.entry("understandingSupport", InfoGroup.HEALTH),
                        Map.entry("assessmentReviewDate", InfoGroup.HEALTH),
                        Map.entry("decisionMakerType", InfoGroup.HEALTH),
                        Map.entry("decisionMakerFamilyId", InfoGroup.HEALTH),
                        Map.entry("decisionMakerDoctorId", InfoGroup.HEALTH),
                        Map.entry("doctorDecisionReason", InfoGroup.HEALTH),
                        Map.entry("reviewDate", InfoGroup.HEALTH),
                        Map.entry("participants", InfoGroup.HEALTH),
                        Map.entry("opinions", InfoGroup.HEALTH)));
    }

    @Bean
    CareFieldClassification fieldsRISK_OPINION() {
        return new CareFieldClassification(
                "M04.RISK_OPINION",
                Map.ofEntries(
                        Map.entry("id", InfoGroup.HEALTH),
                        Map.entry("agreementId", InfoGroup.HEALTH),
                        Map.entry("content", InfoGroup.HEALTH),
                        Map.entry("sourceType", InfoGroup.HEALTH),
                        Map.entry("familyMemberId", InfoGroup.HEALTH),
                        Map.entry("recordedById", InfoGroup.HEALTH),
                        Map.entry("at", InfoGroup.HEALTH)));
    }

    @Bean
    CareFieldClassification fieldsDAY_CARE_ATTENDANCE() {
        return new CareFieldClassification(
                "M04.DAY_CARE_ATTENDANCE",
                Map.ofEntries(
                        Map.entry("status", InfoGroup.INTERNAL_PUBLIC),
                        Map.entry("expectedArrival", InfoGroup.INTERNAL_PUBLIC),
                        Map.entry("expectedReturn", InfoGroup.INTERNAL_PUBLIC),
                        Map.entry("arrivedAt", InfoGroup.INTERNAL_PUBLIC),
                        Map.entry("leftAt", InfoGroup.INTERNAL_PUBLIC)));
    }

    @Bean
    CareFieldClassification fieldsATTENDANCE_RECORD() {
        return new CareFieldClassification(
                "M04.ATTENDANCE_RECORD",
                Map.ofEntries(
                        Map.entry("at", InfoGroup.INTERNAL_PUBLIC), Map.entry("type", InfoGroup.INTERNAL_PUBLIC)));
    }

    @Bean
    CareScopeAssignment scopeCARE_PLAN(CareGovernanceStore store, ObjectProvider<ResidentLocationApi> locations) {
        return new CareScopeAssignment("M04.CARE_PLAN", store, locations);
    }

    @Bean
    CareScopeAssignment scopeTASK(CareGovernanceStore store, ObjectProvider<ResidentLocationApi> locations) {
        return new CareScopeAssignment("M04.TASK", store, locations);
    }

    @Bean
    CareScopeAssignment scopeCARE_RESULT(CareGovernanceStore store, ObjectProvider<ResidentLocationApi> locations) {
        return new CareScopeAssignment("M04.CARE_RESULT", store, locations);
    }

    @Bean
    CareScopeAssignment scopeDAY_CARE_ATTENDANCE(
            CareGovernanceStore store, ObjectProvider<ResidentLocationApi> locations) {
        return new CareScopeAssignment("M04.DAY_CARE_ATTENDANCE", store, locations);
    }

    @Bean
    CareRetention retainCARE_PLAN(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.CARE_PLAN, store, database, residents);
    }

    @Bean
    CareRetention retainCARE_PLAN_ITEM(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.CARE_PLAN_ITEM, store, database, residents);
    }

    @Bean
    CareRetention retainRISK_AGREEMENT(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.RISK_AGREEMENT, store, database, residents);
    }

    @Bean
    CareRetention retainRISK_OPINION(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.RISK_OPINION, store, database, residents);
    }

    @Bean
    CareRetention retainTASK(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.TASK, store, database, residents);
    }

    @Bean
    CareRetention retainLOCATION_TASK(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.LOCATION_TASK, store, database, residents);
    }

    @Bean
    CareRetention retainCARE_RESULT(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.CARE_RESULT, store, database, residents);
    }

    @Bean
    CareRetention retainLOCATION_CARE_RESULT(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.LOCATION_CARE_RESULT, store, database, residents);
    }

    @Bean
    CareRetention retainTASK_HISTORY(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.TASK_HISTORY, store, database, residents);
    }

    @Bean
    CareRetention retainLOCATION_TASK_HISTORY(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.LOCATION_TASK_HISTORY, store, database, residents);
    }

    @Bean
    CareRetention retainDAY_CARE_ATTENDANCE(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.DAY_CARE_ATTENDANCE, store, database, residents);
    }

    @Bean
    CareRetention retainATTENDANCE_RECORD(
            CareGovernanceStore store, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new CareRetention(CareRetentionType.ATTENDANCE_RECORD, store, database, residents);
    }
}
