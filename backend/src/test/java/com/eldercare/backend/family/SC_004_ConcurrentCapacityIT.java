package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * 012 T022 ({@code SC_004_SucChuaDongThoiIT}; Q22, US4 kịch bản 12; FR-030 (b), SC-004): khung còn đúng một chỗ, 20 đăng
 * ký một người cùng lúc thì đúng một lượt được Đã duyệt, các đăng ký còn lại bị từ chối "khung giờ đã đủ sức chứa"; khung
 * không vượt sức chứa.
 */
class SC_004_ConcurrentCapacityIT extends VisitTestBase {

    @Test
    void SC_004_twentyRegistrationsForTheLastSeat_exactlyOneApproved() throws Exception {
        for (int i = 0; i < 6; i++) {
            existingVisit(3);
        }
        existingVisit(1);
        String session = family(mMember);
        String officeSession = office;

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String authorization = i % 2 == 0 ? session : officeSession;
                Long onBehalf = i % 2 == 0 ? null : nMember;
                Callable<Integer> attempt = () -> {
                    start.await();
                    return register(authorization, onBehalf, MORNING_START, MORNING_END)
                            .andReturn()
                            .getResponse()
                            .getStatus();
                };
                results.add(pool.submit(attempt));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
            assertThat(statuses).filteredOn(s -> s == 422).hasSize(19);
        } finally {
            pool.shutdownNow();
        }
        JsonCount.assertSeats(getJson("/api/v1/visit-slots?date=" + day), 0);
    }

    static final class JsonCount {
        static void assertSeats(tools.jackson.databind.JsonNode slots, int morningSeats) {
            assertThat(slots.get(0).get("seatsLeft").asInt()).isEqualTo(morningSeats);
        }
    }
}
