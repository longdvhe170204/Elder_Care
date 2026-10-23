package com.cambae.backend.m09.internal.config;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.credential.internal.persistence.CredentialStore;
import com.cambae.backend.m09.internal.service.*;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

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
    RetentionProvider shiftTemplateRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.SHIFT_TEMPLATE, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider scheduleRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.SCHEDULE, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider shiftRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.SHIFT, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider shiftStaffRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.SHIFT_STAFF, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider shiftLeadRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.SHIFT_LEAD_HISTORY, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider shiftPublicationRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.SHIFT_PUBLICATION, jdbc, database, time, accounts, activities);
    }

    private M09Retention retention(
            M09Retention.Kind kind,
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return new M09Retention(kind, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider staffRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.STAFF, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider credentialRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.CREDENTIAL, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider departmentRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.DEPARTMENT, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider credentialTypeRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.CREDENTIAL_TYPE, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider practiceScopeRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.PRACTICE_SCOPE, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider staffDepartmentRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.STAFF_DEPARTMENT, jdbc, database, time, accounts, activities);
    }

    @Bean
    RetentionProvider departmentLeadHistoryRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        return retention(M09Retention.Kind.DEPARTMENT_LEAD_HISTORY, jdbc, database, time, accounts, activities);
    }
}
