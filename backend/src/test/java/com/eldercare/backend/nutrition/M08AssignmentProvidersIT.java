package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T007 (phần bản gán; research M-15; team-plan mục 8 dòng {@code RetentionProvider}, 000 T112): bản gán lưu giữ theo
 * hồ sơ người cao tuổi, tìm được qua {@code referencing} của hồ sơ, là ứng viên khi người đó sang trạng thái cuối trước
 * mốc; phạm vi của bản gán là người cao tuổi của bản gán.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import(M08AssignmentProvidersIT.Users.class)
class M08AssignmentProvidersIT extends MySqlIntegrationTestBase {

    private static final String TYPE = "M08.DIET_ASSIGNMENT";

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionProvidersUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    jakarta.persistence.EntityManager entityManager;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    NutritionCatalogFixture f;
    long resident;
    long assignment;

    @BeforeEach
    void setUp() throws Exception {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        long base = f.baseDiet();
        long safe = f.activeDish(List.of(), List.of(base));
        long regular = f.diet(false, List.of(), safe);
        resident = f.resident("IN_RESIDENCE");
        assignment = f.activeAssignment(resident, regular, "REGULAR", List.of(), time.now());
    }

    private RetentionProvider provider() {
        return retention.stream()
                .filter(p -> TYPE.equals(p.dataType()))
                .findFirst()
                .orElseThrow();
    }

    /** Mọi trang ứng viên trước mốc (schema giữ dữ liệu của test khác, nên không dựa vào trang đầu). */
    private List<Long> allCandidates(Instant cutoff) {
        List<Long> ids = new ArrayList<>();
        Candidate after = null;
        while (true) {
            List<Candidate> page =
                    provider().expiredCandidates(cutoff, after, 500).toList();
            if (page.isEmpty()) {
                return ids;
            }
            page.forEach(c -> ids.add(c.id()));
            after = page.getLast();
        }
    }

    @Test
    void FR_051a_assignmentsKeptByResidentRecord_foundByReferencingTheResident() {
        assertThat(provider().retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        assertThat(provider().referencing(new DataRef("M01.RESIDENT", resident)))
                .containsExactly(new DataRef(TYPE, assignment));
        assertThat(provider().referencing(new DataRef("M02.CONTRACT", resident)))
                .isEmpty();
    }

    @Test
    void FR_051_candidateOnlyAfterResidentReachesFinalStatus() {
        Instant cutoff = time.now().plus(Duration.ofDays(3650));
        assertThat(allCandidates(cutoff)).doesNotContain(assignment);

        jdbc.update("UPDATE resident SET status = 'DECEASED', status_since = NOW(6) WHERE id = ?", resident);
        // Hồ sơ đã nạp vào persistence context khi lập bản gán; đọc lại sau khi đổi trạng thái bằng SQL
        entityManager.clear();

        assertThat(allCandidates(cutoff)).contains(assignment);
    }

    @Test
    void FR_034_scopeOfAssignmentIsItsResident() {
        ScopeAssignmentProvider scope = scopes.stream()
                .filter(p -> TYPE.equals(p.entityType()))
                .findFirst()
                .orElseThrow();

        assertThat(scope.assignScope(assignment).residentId()).isEqualTo(resident);
    }
}
