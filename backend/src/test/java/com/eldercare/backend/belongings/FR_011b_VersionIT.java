package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * 013 T012 (US2; FR-011b, research K-03; quickstart Q9): lệnh lập trên phiên bản cũ bị từ chối kèm trạng thái mới; hai
 * lệnh đồng thời trên cùng đồ chỉ một thành công.
 */
class FR_011b_VersionIT extends HandoverTestBase {

    @Test
    void Q9_twoConcurrentGiveForUse_onlyOneSucceeds() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        String body = command("GIVE_FOR_USE", item(clothes, ""), "");
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (String token : List.of(tokenA, tokenB)) {
                Callable<Integer> call = () -> {
                    start.await();
                    return handover(token, body).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(statusOf(clothes)).isEqualTo("IN_USE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ? AND kind = 'GIVE_FOR_USE'",
                        Integer.class,
                        clothes))
                .isEqualTo(1);
    }

    @Test
    void FR_011b_staleVersion_isRejected_withCurrentState() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location);
        String stale = command("GIVE_FOR_USE", item(clothes, ""), "");
        handover(tokenA, stale).andExpect(status().isOk());

        handover(tokenA, stale)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"))
                .andExpect(jsonPath("$.currentValue[0].status").value("IN_USE"));
        assertThat(version(clothes)).isEqualTo(2);
    }
}
