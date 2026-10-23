package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.CareLevelAtDate;
import com.cambae.backend.m01.api.dto.CareLevelBasisType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.CareLevelChanged;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T061, T065 (US6 #11; FR-052, FR-053): mức chăm sóc đổi theo yêu cầu thay đổi lưu trú ghi dòng lịch sử có căn cứ
 * {@code RESIDENCY_CHANGE_REQUEST} hiệu lực từ 00:00 ngày hiệu lực (giờ Việt Nam); tra theo ngày trả đúng mức và căn cứ.
 */
class FR_053_MucTaiNgayIT extends StatusTestBase {

    @Autowired
    TestClockControl clock;

    @Autowired
    CareLevelApi careLevels;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    @Test
    void US6_11_levelAtDateFollowsTheHistory_withTheBasisOfEachChange() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        clock.setTo(Instant.parse("2027-03-01T03:00:00Z"));
        long assessmentId = confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Đầu vào", null)
                .id();
        moveTo(id, ResidentStatus.IN_RESIDENCE);

        clock.setTo(Instant.parse("2027-06-10T03:00:00Z"));
        user.set(office.user());
        inTransaction(() -> {
            careLevels.applyChange(id, CareLevel.SPECIAL, LocalDate.of(2027, 6, 15), 777L);
            return null;
        });

        CareLevelAtDate beforeChange =
                careLevels.atDate(id, LocalDate.of(2027, 6, 1)).orElseThrow();
        assertThat(beforeChange.careLevel()).isEqualTo(CareLevel.BASIC);
        assertThat(beforeChange.basisType()).isEqualTo(CareLevelBasisType.ASSESSMENT);
        assertThat(beforeChange.basisId()).isEqualTo(assessmentId);

        CareLevelAtDate afterChange =
                careLevels.atDate(id, LocalDate.of(2027, 6, 20)).orElseThrow();
        assertThat(afterChange.careLevel()).isEqualTo(CareLevel.SPECIAL);
        assertThat(afterChange.basisType()).isEqualTo(CareLevelBasisType.RESIDENCY_CHANGE_REQUEST);
        assertThat(afterChange.basisId()).isEqualTo(777L);
        assertThat(afterChange.effectiveFrom())
                .as("00:00 ngày 15/06 giờ Việt Nam")
                .isEqualTo(Instant.parse("2027-06-14T17:00:00Z"));

        assertThat(careLevels
                        .atDate(id, LocalDate.of(2027, 6, 14))
                        .orElseThrow()
                        .careLevel())
                .as("ngày liền trước ngày hiệu lực")
                .isEqualTo(CareLevel.BASIC);
        assertThat(careLevels
                        .atDate(id, LocalDate.of(2027, 6, 15))
                        .orElseThrow()
                        .careLevel())
                .isEqualTo(CareLevel.SPECIAL);
        assertThat(careLevels.atDate(id, LocalDate.of(2027, 2, 28)))
                .as("trước lần đầu có mức")
                .isEmpty();
        assertThat(events.stream(CareLevelChanged.class))
                .anyMatch(e -> e.residentId() == id && e.basisType() == CareLevelBasisType.RESIDENCY_CHANGE_REQUEST);
    }
}
