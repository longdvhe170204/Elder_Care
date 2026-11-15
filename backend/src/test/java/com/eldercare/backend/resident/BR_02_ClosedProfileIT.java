package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 001 T045 (BR-M01-05, FR-049, US4 #12): hồ sơ ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận) chặn mọi lệnh
 * chuyển trạng thái, không thêm dòng lịch sử; đọc lịch sử vẫn được.
 */
class BR_02_ClosedProfileIT extends StatusTestBase {

    private static final List<ResidentStatus> FINAL_STATUSES =
            List.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    @Test
    void BR_02_everyUserCommandIsBlockedOnAFinalStatus_andNothingIsWritten() {
        for (ResidentStatus finalStatus : FINAL_STATUSES) {
            long id = residentIn(finalStatus);
            int rows = historyCount(id);
            List<Consumer<Long>> commands = List.of(
                    r -> asOffice(() -> statusService.completeAdmission(target(r), reason(), noDetails())),
                    r -> asOffice(() -> statusService.cancelAdmission(target(r), reason(), noDetails())),
                    r -> asOffice(() -> statusService.grantTemporaryAbsence(target(r), reason(), noDetails())),
                    r -> asOffice(() -> statusService.recordReturn(target(r), reason(), noDetails())),
                    r -> {
                        user.set(doctor.user());
                        statusService.transferOut(target(r), reason(), noDetails());
                    },
                    r -> asOffice(() -> statusService.endResidency(target(r), reason(), noDetails())),
                    r -> asOffice(() -> statusService.recordDeath(target(r), reason(), noDetails())));

            for (Consumer<Long> command : commands) {
                BusinessException e = failure(() -> command.accept(id));

                assertThat(e).as("%s", finalStatus).isNotNull();
                assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED");
            }
            assertThat(historyCount(id)).isEqualTo(rows);
            assertThat(currentStatus(id)).isEqualTo(finalStatus);
        }
    }

    private void asOffice(Runnable command) {
        user.set(office.user());
        command.run();
    }
}
