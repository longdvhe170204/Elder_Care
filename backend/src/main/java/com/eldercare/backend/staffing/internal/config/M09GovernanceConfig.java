package com.eldercare.backend.staffing.internal.config;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.staffing.internal.credential.persistence.CredentialStore;
import com.eldercare.backend.staffing.internal.persistence.M09Retention;
import com.eldercare.backend.staffing.internal.persistence.M09RetentionFactory;
import com.eldercare.backend.staffing.internal.service.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import java.util.Map;
import org.springframework.context.annotation.*;

/** T009: đăng ký provider dùng chung, không đọc bảng của module khác. */
@Configuration(proxyBeanMethods = false)
class M09GovernanceConfig {
    private static FieldClassificationProvider fields(String type, Map<String, InfoGroup> groups) {
        return new FieldClassificationProvider() {
            public String entityType() {
                return type;
            }

            public Map<String, InfoGroup> fieldGroups() {
                return groups;
            }
        };
    }

    @Bean
    FieldClassificationProvider staffFields() {
        return fields(
                "M09.STAFF",
                Map.of(
                        "phoneNumber",
                        InfoGroup.STAFF_CONTACT,
                        "email",
                        InfoGroup.STAFF_CONTACT,
                        "address",
                        InfoGroup.STAFF_CONTACT,
                        "terminationReason",
                        InfoGroup.STAFF_RESTRICTED));
    }

    @Bean
    FieldClassificationProvider credentialFields() {
        return fields(
                "M09.CREDENTIAL",
                Map.of(
                        "credentialNumber",
                        InfoGroup.STAFF_RESTRICTED,
                        "scanFileId",
                        InfoGroup.STAFF_RESTRICTED,
                        "revocationReason",
                        InfoGroup.STAFF_RESTRICTED,
                        "correctionReason",
                        InfoGroup.STAFF_RESTRICTED));
    }

    @Bean
    FieldClassificationProvider absenceFields() {
        return fields("M09.STAFF_ABSENCE", Map.of("reason", InfoGroup.STAFF_REASON));
    }

    @Bean
    FieldClassificationProvider handoverFields() {
        return fields(
                "M09.HANDOVER",
                Map.of(
                        "notes",
                        InfoGroup.HEALTH,
                        "assessment",
                        InfoGroup.HEALTH,
                        "description",
                        InfoGroup.HEALTH,
                        "feedback",
                        InfoGroup.HEALTH,
                        "reply",
                        InfoGroup.HEALTH));
    }

    @Bean
    ScopeAssignmentProvider staffScope(StaffStore staff, CredentialStore credentials) {
        return new M09ScopeAssignment("M09.STAFF", staff, credentials);
    }

    @Bean
    ScopeAssignmentProvider credentialScope(StaffStore staff, CredentialStore credentials) {
        return new M09ScopeAssignment("M09.CREDENTIAL", staff, credentials);
    }

    @Bean
    ScopeAssignmentProvider shiftScope(ShiftStore store) {
        return new ShiftScopeAssignment("M09.SHIFT", store);
    }

    @Bean
    ScopeAssignmentProvider scheduleScope(ShiftStore store) {
        return new ShiftScopeAssignment("M09.SCHEDULE", store);
    }

    @Bean
    ScopeAssignmentProvider assignmentScope(
            com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore assignments,
            ShiftStore shifts) {
        return new AssignmentScopeAssignment("M09.ASSIGNMENT", assignments, shifts);
    }

    @Bean
    ScopeAssignmentProvider floorManagerAssignmentScope(
            com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore assignments,
            ShiftStore shifts) {
        return new AssignmentScopeAssignment("M09.FLOOR_MANAGER_ASSIGNMENT", assignments, shifts);
    }

    @Bean
    RetentionProvider shiftTemplateRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.SHIFT_TEMPLATE);
    }

    @Bean
    RetentionProvider scheduleRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.SCHEDULE);
    }

    @Bean
    RetentionProvider shiftRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.SHIFT);
    }

    @Bean
    RetentionProvider shiftStaffRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.SHIFT_STAFF);
    }

    @Bean
    RetentionProvider shiftLeadRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.SHIFT_LEAD_HISTORY);
    }

    @Bean
    RetentionProvider shiftPublicationRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.SHIFT_PUBLICATION);
    }

    @Bean
    RetentionProvider assignmentRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.ASSIGNMENT);
    }

    @Bean
    RetentionProvider floorManagerAssignmentRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.FLOOR_MANAGER_ASSIGNMENT);
    }

    @Bean
    RetentionProvider staffRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.STAFF);
    }

    @Bean
    RetentionProvider credentialRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.CREDENTIAL);
    }

    @Bean
    RetentionProvider departmentRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.DEPARTMENT);
    }

    @Bean
    RetentionProvider credentialTypeRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.CREDENTIAL_TYPE);
    }

    @Bean
    RetentionProvider practiceScopeRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.PRACTICE_SCOPE);
    }

    @Bean
    RetentionProvider staffDepartmentRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.STAFF_DEPARTMENT);
    }

    @Bean
    RetentionProvider departmentLeadHistoryRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.DEPARTMENT_LEAD_HISTORY);
    }

    @Bean
    RetentionProvider staffingRatioRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.STAFFING_RATIO_CALCULATION);
    }

    @Bean
    RetentionProvider staffImportRetention(M09RetentionFactory retention) {
        return retention.create(M09Retention.Kind.STAFF_IMPORT_BATCH);
    }
}
