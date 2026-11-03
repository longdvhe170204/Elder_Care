package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(FakeM01Config.class)
class T008_CareGovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    List<FieldClassificationProvider> fields;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeResidents residents;

    @Test
    void FR_051_profileAndUnlinkedCandidatesUseDifferentClocksAndRetentionConnection() throws Exception {
        // Phase4 đã có bảng thật; fixture chỉ thêm/xóa các id riêng, không tạo hoặc drop bảng.
        long planId = TestUsers.nextId(),
                residentId = TestUsers.nextId(),
                linkedId = TestUsers.nextId(),
                locationId = TestUsers.nextId(),
                floor = TestUsers.nextId();
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            try {
                jdbc.update(
                        "INSERT INTO care_plan(id,resident_id,revision,status,created_by,created_at) VALUES (?,?,1,'DRAFT',1,'1999-01-01')",
                        planId,
                        residentId);
                String insert =
                        "INSERT INTO care_task(id,resident_id,location_type,location_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,status,data,created_by,created_at) VALUES(?,?,?,?,?, ?,?,?,'1999-01-01','1999-01-01','1999-01-01','NORMAL','NURSING_STAFF','CANCELLED','{}',1,'1999-01-01')";
                jdbc.update(
                        insert,
                        linkedId,
                        residentId,
                        null,
                        null,
                        TestUsers.nextId(),
                        "AD_HOC",
                        TestUsers.nextId(),
                        floor);
                jdbc.update(
                        insert,
                        locationId,
                        null,
                        "FLOOR",
                        floor,
                        TestUsers.nextId(),
                        "SHIFT_START_CHECK",
                        TestUsers.nextId(),
                        floor);
                residents
                        .resident(residentId, "[Họ tên NCT 1]", Gender.MALE, ResidentStatus.RESIDENCY_ENDED)
                        .changeStatus(
                                residentId, ResidentStatus.RESIDENCY_ENDED, Instant.parse("2000-01-01T00:00:00Z"));
                var cutoff = Instant.parse("2001-01-01T00:00:00Z");
                var plan = retention.stream()
                        .filter(p -> p.dataType().equals("M04.CARE_PLAN"))
                        .findFirst()
                        .orElseThrow();
                assertThat(plan.expiredCandidates(cutoff, 10000).map(RetentionProvider.Candidate::id))
                        .contains(planId);
                var task = retention.stream()
                        .filter(p -> p.dataType().equals("M04.TASK"))
                        .findFirst()
                        .orElseThrow();
                assertThat(task.expiredCandidates(cutoff, 10000).map(RetentionProvider.Candidate::id))
                        .contains(linkedId)
                        .doesNotContain(locationId);
                var unlinked = retention.stream()
                        .filter(p -> p.dataType().equals("M04.LOCATION_TASK"))
                        .findFirst()
                        .orElseThrow();
                assertThat(unlinked.expiredCandidates(cutoff, 10000).map(RetentionProvider.Candidate::id))
                        .contains(locationId)
                        .doesNotContain(linkedId);
                assertThat(task.expiredCandidates(Instant.parse("1999-12-31T00:00:00Z"), 10000)
                                .map(RetentionProvider.Candidate::id))
                        .doesNotContain(linkedId);
                var scope = scopes.stream()
                        .filter(p -> p.entityType().equals("M04.TASK"))
                        .findFirst()
                        .orElseThrow()
                        .assignScope(locationId);
                assertThat(scope.residentId()).isNull();
                assertThat(scope.floorAreaId()).isEqualTo(floor);
                task.delete(List.of(linkedId));
                unlinked.delete(List.of(locationId));
                assertThat(jdbc.queryForObject(
                                "SELECT COUNT(*) FROM care_task WHERE id IN (?,?)",
                                Integer.class,
                                linkedId,
                                locationId))
                        .isZero();
                plan.delete(List.of(planId));
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_plan WHERE id=?", Integer.class, planId))
                        .isZero();
            } finally {
                statement.execute("DELETE FROM care_task WHERE id IN (" + linkedId + "," + locationId + ")");
                statement.execute("DELETE FROM care_plan WHERE id=" + planId);
            }
        }
    }

    @Test
    void FR_049_sensitiveFieldsAreHealthAndTechnicalStateRemainsInternal() {
        for (String type : List.of("M04.CARE_PLAN", "M04.TASK", "M04.CARE_RESULT", "M04.RISK_AGREEMENT")) {
            var provider =
                    fields.stream().filter(p -> p.entityType().equals(type)).findFirst();
            assertThat(provider).isPresent();
            String field = switch (type) {
                case "M04.CARE_PLAN", "M04.TASK" -> "content";
                case "M04.CARE_RESULT" -> "values";
                default -> "choice";
            };
            assertThat(provider.get().fieldGroups()).containsEntry(field, InfoGroup.HEALTH);
        }
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M04.DAY_CARE_ATTENDANCE"))
                        .findFirst())
                .isPresent();
    }

    @Test
    void FR_002a_riskAgreementIdentityAndRecordingMetadataAreHealthToo() {
        var agreement = fields.stream()
                .filter(p -> p.entityType().equals("M04.RISK_AGREEMENT"))
                .findFirst()
                .orElseThrow();
        for (String field : List.of("id", "carePlanId", "sourceAgreementId", "recordedById", "recordedAt"))
            assertThat(agreement.fieldGroups()).containsEntry(field, InfoGroup.HEALTH);
        var opinion = fields.stream()
                .filter(p -> p.entityType().equals("M04.RISK_OPINION"))
                .findFirst()
                .orElseThrow();
        for (String field : List.of("id", "agreementId"))
            assertThat(opinion.fieldGroups()).containsEntry(field, InfoGroup.HEALTH);
    }

    @Test
    void FR_034_scopeProvidersRejectMissingRecordInsteadOfFacilityWideScope() {
        for (String type : List.of("M04.CARE_PLAN", "M04.TASK", "M04.CARE_RESULT", "M04.DAY_CARE_ATTENDANCE")) {
            var provider =
                    scopes.stream().filter(p -> p.entityType().equals(type)).findFirst();
            assertThat(provider).isPresent();
            assertThatThrownBy(() -> provider.get().assignScope(Long.MAX_VALUE))
                    .isInstanceOf(InvalidPermissionDataException.class);
        }
    }

    @Test
    void FR_050_retentionDoesNotQueryUncreatedStoryTablesAndSeparatesUnlinkedTasks() {
        for (String type : List.of("M04.CARE_PLAN", "M04.TASK", "M04.CARE_RESULT", "M04.DAY_CARE_ATTENDANCE")) {
            var provider =
                    retention.stream().filter(p -> p.dataType().equals(type)).findFirst();
            assertThat(provider).isPresent();
            assertThat(provider.get().retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
            assertThat(provider.get().expiredCandidates(Instant.parse("2000-01-01T00:00:00Z"), 10))
                    .isEmpty();
        }
        var unlinked = retention.stream()
                .filter(p -> p.dataType().equals("M04.LOCATION_TASK"))
                .findFirst();
        assertThat(unlinked).isPresent();
        assertThat(unlinked.get().retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void FR_050_retentionSeparatesTaskAndRiskKindsAndTraversesBothFromThePlan() {
        long residentId = TestUsers.nextId(),
                planId = TestUsers.nextId(),
                riskId = TestUsers.nextId(),
                taskId = TestUsers.nextId(),
                typeId = TestUsers.nextId();
        residents
                .resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.RESIDENCY_ENDED)
                .changeStatus(residentId, ResidentStatus.RESIDENCY_ENDED, Instant.parse("2000-01-01T00:00:00Z"));
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,created_by,created_at) VALUES(?,?,1,'DRAFT',1,'1999-01-01')",
                planId,
                residentId);
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE','{}')",
                typeId,
                "Loại retention " + typeId);
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,task_type_id,frequency_type,data) VALUES(?,?,'TASK',?,'ONCE_PER_SHIFT','{}')",
                taskId,
                planId,
                typeId);
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,data,choice,discussed_risks,resident_participates,assessing_doctor_id,can_self_determine,support_method,assessment_at,assessment_review_date,decision_maker_type,review_date) VALUES(?,?,'RISK_AGREEMENT','{}','Lựa chọn','Đã trao đổi',TRUE,1,TRUE,'Đã hỗ trợ','2026-10-31','2026-11-30','RESIDENT','2026-11-30')",
                riskId,
                planId);
        var providers =
                retention.stream().collect(java.util.stream.Collectors.toMap(RetentionProvider::dataType, p -> p));
        var cutoff = Instant.parse("2001-01-01T00:00:00Z");
        assertThat(providers
                        .get("M04.CARE_PLAN_ITEM")
                        .expiredCandidates(cutoff, 100)
                        .map(RetentionProvider.Candidate::id))
                .contains(taskId)
                .doesNotContain(riskId);
        assertThat(providers
                        .get("M04.RISK_AGREEMENT")
                        .expiredCandidates(cutoff, 100)
                        .map(RetentionProvider.Candidate::id))
                .contains(riskId)
                .doesNotContain(taskId);
        assertThat(providers.get("M04.CARE_PLAN").referencesTo(planId))
                .contains(
                        new RetentionProvider.DataRef("M04.CARE_PLAN_ITEM", taskId),
                        new RetentionProvider.DataRef("M04.RISK_AGREEMENT", riskId));
    }
}
