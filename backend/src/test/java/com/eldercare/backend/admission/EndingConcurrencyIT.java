package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.internal.ending.service.ResidencyEndingService;
import com.eldercare.backend.admission.internal.ending.service.ResidencyEndingService.NewEndingInput;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TableRows;
import java.time.Instant;
import java.util.List;
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

    /** Lặp 8 lần để bắt tranh chấp; mỗi lần dựng lại trạng thái như một lần của {@code @RepeatedTest} trước đây. */
    @Test
    void FR_062_twoConcurrentCreates_exactlyOneEndingIsPreparing() {
        TableRows.each(IntStream.rangeClosed(1, 8).boxed(), run -> concurrentCreates());
    }

    private void concurrentCreates() throws Exception {
        endingSetup();
        long residentId = inResidence();
        user.set(office.user());

        CompletableFuture<Boolean> one = CompletableFuture.supplyAsync(() -> creates(residentId, 5));
        CompletableFuture<Boolean> two = CompletableFuture.supplyAsync(() -> creates(residentId, 6));

        assertThat(List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM residency_ending WHERE kind = 'ENDING' AND resident_id = ? AND status = 'PREPARING'",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
        assertThat(finalPeriods.calls()).as("chỉ lệnh thắng gọi 010").hasSize(1);
    }

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
