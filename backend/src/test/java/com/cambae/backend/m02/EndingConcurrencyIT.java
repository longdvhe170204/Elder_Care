package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.EndCase;
import com.cambae.backend.m02.internal.ending.service.ResidencyEndingService;
import com.cambae.backend.m02.internal.ending.service.ResidencyEndingService.NewEndingInput;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
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

    @RepeatedTest(8)
    void FR_062_twoConcurrentCreates_exactlyOneEndingIsPreparing() throws Exception {
        long residentId = inResidence();
        user.set(office.user());

        CompletableFuture<Boolean> one = CompletableFuture.supplyAsync(() -> creates(residentId, 5));
        CompletableFuture<Boolean> two = CompletableFuture.supplyAsync(() -> creates(residentId, 6));

        assertThat(List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM residency_ending WHERE resident_id = ? AND status = 'PREPARING'",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
        assertThat(finalPeriods.calls()).as("chỉ lệnh thắng gọi 010").hasSize(1);
    }

    @RepeatedTest(4)
    void FR_067_cancelRacingWithTheEndCommand_leavesOneConsistentOutcome() throws Exception {
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

    /** true khi lập được, false khi bị chặn vì đã có hồ sơ Đang chuẩn bị. */
    private boolean creates(long residentId, int days) {
        try {
            endings.create(
                    ResidencyEndingService.targetForResident(residentId),
                    new NewEndingInput(residentId, EndCase.DISCHARGE, false, d0.plusDays(days), null, "Người nhà"),
                    new Reason("Gia đình đưa về"));
            return true;
        } catch (BusinessException e) {
            assertThat(e.errorCode()).isEqualTo("RESIDENCY_ENDING_ALREADY_OPEN");
            return false;
        }
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
