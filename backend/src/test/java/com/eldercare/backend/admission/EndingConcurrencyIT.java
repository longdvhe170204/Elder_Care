package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.ending.service.ResidencyEndingService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TableRows;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T057 (US7; FR-062, FR-065, FR-067; research T-03): các lệnh trên cùng người cao tuổi chạy tuần tự dưới khóa {@code
 * resident_residency}. Hai lệnh lập hồ sơ đồng thời chỉ một thành công; hủy hồ sơ đồng thời với lệnh Kết thúc lưu trú chỉ để
 * lại một kết quả nhất quán.
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingConcurrencyIT extends EndingTestBase {

    @Autowired
    ResidencyEndingService endings;

    @Test
    void FR_067_cancelRacingWithTheEndCommand_leavesOneConsistentOutcome() {
        TableRows.each(IntStream.rangeClosed(1, 4).boxed(), run -> cancelRacingEnd());
    }

    private void cancelRacingEnd() throws Exception {
        endingSetup();
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        Instant effectiveAt = at(d0.plusDays(1), 12, 0);
        user.set(office.user());

        CompletableFuture<Boolean> end = CompletableFuture.supplyAsync(() -> ends(ready, effectiveAt));
        CompletableFuture<Boolean> cancel = CompletableFuture.supplyAsync(() -> cancels(ready));

        boolean ended = end.get(30, TimeUnit.SECONDS);
        boolean cancelled = cancel.get(30, TimeUnit.SECONDS);
        assertThat(ended ^ cancelled).as("đúng một lệnh thắng").isTrue();
        assertThat(endingStatus(ready.endingId())).isEqualTo(ended ? "COMPLETED" : "CANCELLED");
        assertThat(residentStatus(ready.residentId()))
                .isEqualTo(ended ? ResidentStatus.RESIDENCY_ENDED : ResidentStatus.IN_RESIDENCE);
    }

    private boolean ends(ReadyEnding ready, Instant effectiveAt) {
        return endResidencyFailure(ready.residentId(), ready.endingId(), effectiveAt) == null;
    }

    private boolean cancels(ReadyEnding ready) {
        try {
            endings.cancel(endings.targetOf(ready.endingId()), ready.endingId(), new Reason("Gia đình đổi ý"));
            return true;
        } catch (BusinessException e) {
            assertThat(e.errorCode()).isEqualTo("RESIDENCY_ENDING_NOT_PREPARING");
            return false;
        }
    }
}
