package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 011 T012 (US1, BR-M08-03, Edge Cases "hai Bác sĩ cùng áp dụng"; 000 FR-040): hai lệnh Áp dụng cùng một đề xuất chạy
 * đồng thời thì chỉ một lệnh được ghi nhận, lệnh sau nhận 409 {@code STATE_CHANGED}; người cao tuổi có đúng một bản
 * Hiệu lực. Không {@code @Transactional}: dữ liệu phải commit để hai lệnh thấy nhau; schema giữ dữ liệu giữa các lần chạy,
 * người cao tuổi mới mỗi lần nên không giả định bảng rỗng.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(BR_M08_03_ConcurrentApplyIT.Users.class)
class BR_M08_03_ConcurrentApplyIT extends MySqlIntegrationTestBase {

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionConcurrentUser() {
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

    @Test
    void BR_M08_03_twoDoctorsApplyAtOnce_onlyOneSucceeds() throws Exception {
        NutritionCatalogFixture f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        long base = f.baseDiet();
        long safe = f.activeDish(List.of(), List.of(base));
        long regular = f.diet(false, List.of(), safe);
        long resident = f.resident("IN_RESIDENCE");
        f.propose(resident, NutritionCatalogFixture.assignmentBody(regular, "REGULAR", List.of(), time.now()))
                .andExpect(status().isCreated());
        long proposal = f.latestAssignment(resident);
        f.asDoctor();

        SecurityContext security = TestSecurityContextHolder.getContext();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> apply = () -> {
            TestSecurityContextHolder.setContext(security);
            start.await();
            return f.apply(proposal, null).andReturn().getResponse().getStatus();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Integer> statuses = new ArrayList<>();
        try {
            List<Future<Integer>> results = List.of(pool.submit(apply), pool.submit(apply));
            start.countDown();
            for (Future<Integer> r : results) {
                statuses.add(r.get());
            }
        } finally {
            pool.shutdown();
        }

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM diet_assignment WHERE resident_id = ? AND status = 'ACTIVE'",
                        Integer.class,
                        resident))
                .isEqualTo(1);
    }
}
