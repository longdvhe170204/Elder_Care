package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 012 T008 (data-model mục 2; research F-17; FR-068, FR-001): số giấy tờ của người thân là trường nhận dạng khi xem
 * nhật ký; quan hệ lưu giữ theo hồ sơ người cao tuổi, lịch sử quyền xóa cùng quan hệ, hồ sơ người thân xóa khi không còn
 * quan hệ nào.
 */
class T008_M10ProvidersIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    RelationshipHistoryStore history;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<FieldClassificationProvider> classifications;

    private RelationshipFixtures fixtures;

    @BeforeEach
    void setUp() {
        Instant start = time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS);
        fixtures = new RelationshipFixtures(jdbc, tx, history, start);
    }

    private RetentionProvider relationships() {
        return retention.stream()
                .filter(p -> RelationshipAttribute.ENTITY_TYPE.equals(p.dataType()))
                .findFirst()
                .orElseThrow();
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void F_17_delete_removesRelationshipsAndOrphanFamilyMembers() {
        long resident = TestUsers.nextId();
        long otherResident = TestUsers.nextId();
        long only = fixtures.familyMember("Hồ Văn A", "0903000001");
        long shared = fixtures.familyMember("Hồ Thị B", "0903000002");
        long relOnly = fixtures.relate(only, resident, 1, true, true, FamilyRight.VIEW_COSTS);
        long relShared = fixtures.relate(shared, resident, 2, false, false, FamilyRight.PICKUP_ALLOWED);
        long kept = fixtures.relate(shared, otherResident, 1, true, true);

        relationships().delete(List.of(relOnly, relShared));

        assertThat(count("SELECT COUNT(*) FROM family_relationship WHERE resident_id = ?", resident))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM family_member WHERE id = ?", only))
                .as("không còn quan hệ nào")
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM family_member WHERE id = ?", shared))
                .as("còn quan hệ với người cao tuổi khác")
                .isOne();
        assertThat(count("SELECT COUNT(*) FROM family_relationship WHERE id = ?", kept))
                .isOne();
        assertThat(relationships().referencesTo(relOnly)).isEmpty();
    }
}
