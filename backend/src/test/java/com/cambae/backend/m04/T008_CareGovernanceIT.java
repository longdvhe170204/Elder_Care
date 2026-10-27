package com.cambae.backend.m04;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m01.api.dto.*;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.spi.*;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m01.*;
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
        // Fixture DDL của chính M04 cho provider tương lai, không đưa schema story vào migration Phase2.
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE care_plan(id BIGINT PRIMARY KEY,resident_id BIGINT NOT NULL)");
            try {
                statement.execute(
                        "CREATE TABLE care_task(id BIGINT PRIMARY KEY,resident_id BIGINT NULL,floor_area_id BIGINT NOT NULL,planned_at DATETIME(6) NOT NULL)");
                try {
                    statement.execute("INSERT INTO care_plan VALUES (1,99110)");
                    statement.execute("INSERT INTO care_task VALUES (2,99110,7,'1999-01-01'),(3,NULL,7,'1999-01-01')");
                    statement.execute("GRANT DELETE ON care_plan TO 'eldercare_retention'@'%'");
                    residents
                            .resident(99110, "[Họ tên NCT 1]", Gender.MALE, ResidentStatus.RESIDENCY_ENDED)
                            .changeStatus(99110, ResidentStatus.RESIDENCY_ENDED, Instant.parse("2000-01-01T00:00:00Z"));
                    var cutoff = Instant.parse("2001-01-01T00:00:00Z");
                    var plan = retention.stream()
                            .filter(p -> p.dataType().equals("M04.CARE_PLAN"))
                            .findFirst()
                            .orElseThrow();
                    assertThat(plan.expiredCandidates(cutoff, 10).map(RetentionProvider.Candidate::id))
                            .containsExactly(1L);
                    var task = retention.stream()
                            .filter(p -> p.dataType().equals("M04.TASK"))
                            .findFirst()
                            .orElseThrow();
                    assertThat(task.expiredCandidates(cutoff, 10).map(RetentionProvider.Candidate::id))
                            .containsExactly(2L);
                    var unlinked = retention.stream()
                            .filter(p -> p.dataType().equals("M04.LOCATION_TASK"))
                            .findFirst()
                            .orElseThrow();
                    assertThat(unlinked.expiredCandidates(cutoff, 10).map(RetentionProvider.Candidate::id))
                            .containsExactly(3L);
                    assertThat(task.expiredCandidates(Instant.parse("1999-12-31T00:00:00Z"), 10))
                            .isEmpty();
                    var scope = scopes.stream()
                            .filter(p -> p.entityType().equals("M04.TASK"))
                            .findFirst()
                            .orElseThrow()
                            .assignScope(3);
                    assertThat(scope.residentId()).isNull();
                    assertThat(scope.floorAreaId()).isEqualTo(7L);
                    plan.delete(List.of(1L));
                    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_plan", Integer.class))
                            .isZero();
                } finally {
                    statement.execute("DROP TABLE care_task");
                }
            } finally {
                statement.execute("DROP TABLE care_plan");
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
}
