package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 001 T044 (FR-048, US4 #13; research H-02 mục 2): hai lệnh gửi gần như đồng thời trên cùng một hồ sơ, chỉ lệnh đầu được
 * áp dụng, lệnh sau kiểm tra lại trên trạng thái mới; {@code resident.status} luôn bằng dòng lịch sử mới nhất.
 */
class FR_048_ConcurrentCommandsIT extends StatusTestBase {

    @Test
    void FR_048_twoSimultaneousCommands_exactlyOneSucceeds_theOtherSeesTheNewState() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        int rows = historyCount(id);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<BusinessException> errors = new ArrayList<>();
        try {
            List<Future<BusinessException>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Callable<BusinessException> call = () -> {
                    start.await();
                    return failure(() -> statusService.grantTemporaryAbsence(target(id), reason(), noDetails()));
                };
                results.add(pool.submit(call));
            }
            for (Future<BusinessException> result : results) {
                errors.add(result.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(errors).filteredOn(e -> e == null).hasSize(1);
        assertThat(errors).filteredOn(e -> e != null).singleElement().satisfies(e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.errorCode()).isEqualTo("INVALID_TRANSITION");
        });
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(historyCount(id)).isEqualTo(rows + 1);
        assertThat(latestHistoryStatus(id)).isEqualTo(currentStatus(id));
        assertThat(one.applied()).as("tác động chỉ áp một lần").hasSize(1);
    }
}
