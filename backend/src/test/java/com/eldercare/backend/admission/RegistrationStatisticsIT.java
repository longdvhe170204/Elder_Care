package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.ResidenceRegistrationStatisticsApi;
import com.eldercare.backend.admission.api.dto.RegistrationSummaryView;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 018 T015 (FR-011; module-interfaces mục 2): bốn số cho 016 đếm theo người, đo bằng hiệu so với số đã có vì schema test giữ
 * dữ liệu qua các lần chạy.
 */
@Import(FakeFileConfig.class)
class RegistrationStatisticsIT extends RegistrationTestBase {

    @Autowired
    ResidenceRegistrationStatisticsApi statistics;

    @Autowired
    TestClockControl clock;

    @Autowired
    FileApi files;

    private long evidence() {
        return files.upload(
                "ket-qua.pdf", FileTypes.PDF, "%PDF-1.4 ket qua".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void FR_011_aNewObligation_countsToCreate_thenOverdue_thenStopsOnceSubmitted() {
        RegistrationSummaryView before = statistics.summary();
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);

        RegistrationSummaryView created = statistics.summary();
        assertThat(created.toCreate()).isEqualTo(before.toCreate() + 1);
        assertThat(created.overdue()).isEqualTo(before.overdue());

        clock.setTo(time.now().plus(Duration.ofDays(30)));
        RegistrationSummaryView late = statistics.summary();
        // Dịch đồng hồ làm cả nghĩa vụ sót của test khác quá hạn: kiểm bất biến, không kiểm số tuyệt đối
        assertThat(late.overdue()).isGreaterThan(created.overdue());
        assertThat(late.overdue() + late.toCreate())
                .as("quá hạn thì chuyển nhóm, không đếm hai lần")
                .isEqualTo(created.overdue() + created.toCreate());

        clock.reset();
        ResidenceRegistration registration = createFor(residentId);
        assertThat(statistics.summary().toCreate())
                .as("khai báo Cần khai báo vẫn đếm")
                .isEqualTo(before.toCreate() + 1);
        confirm(registration, today().plusYears(1), evidence());
        RegistrationSummaryView done = statistics.summary();
        assertThat(done.toCreate()).isEqualTo(before.toCreate());
        assertThat(done.overdue()).isEqualTo(before.overdue());
    }
}
