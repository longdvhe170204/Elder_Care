package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusPeriod;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T049 (module-interfaces mục 1.2 {@code khoangTrangThai}; 003 FR-027 c, FR-036): {@code statusPeriods} trả các khoảng
 * người cao tuổi ở Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài, dựng từ lịch sử trạng thái. Mỗi khoảng bắt đầu
 * ở thời điểm vào trạng thái và kết thúc ở dòng lịch sử kế tiếp (rỗng nếu đang ở); lấy khoảng giao với [from, to], trả
 * nguyên khoảng (không cắt) để người gọi tự tính phần giao.
 */
class StatusPeriodsIT extends StatusTestBase {

    private static final Instant T0 = Instant.parse("2030-10-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2030-10-03T08:00:00Z");
    private static final Instant T2 = Instant.parse("2030-10-05T08:00:00Z");
    private static final Instant T3 = Instant.parse("2030-10-10T08:00:00Z");
    private static final Instant END = Instant.parse("2031-12-31T00:00:00Z");

    private static final Set<ResidentStatus> ABSENT_KINDS =
            Set.of(ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED, ResidentStatus.OFF_SITE_ACTIVITY);

    /** Bảng lịch sử chỉ ghi thêm nên các mốc đặt ở tương lai, sau dòng Tạo hồ sơ. Hồ sơ có lịch sử: vào viện T0, Tạm vắng T1, về T2, Điều trị tại bệnh viện T3 (đang ở). */
    private long residentWithHistory() {
        long id = admissionResident();
        row(id, "ADMISSION_IN_PROGRESS", "IN_RESIDENCE", T0.plusSeconds(1));
        row(id, "IN_RESIDENCE", "TEMPORARILY_ABSENT", T1);
        row(id, "TEMPORARILY_ABSENT", "IN_RESIDENCE", T2);
        row(id, "IN_RESIDENCE", "HOSPITALIZED", T3);
        jdbc.update("UPDATE resident SET status = 'HOSPITALIZED' WHERE id = ?", id);
        return id;
    }

    private void row(long id, String from, String to, Instant at) {
        jdbc.update(
                "INSERT INTO resident_status_history (resident_id, from_status, to_status, command, occurred_at,"
                        + " performed_by, by_system) VALUES (?, ?, ?, 'SEED', ?, ?, TRUE)",
                id,
                from,
                to,
                Timestamp.from(at),
                office.accountId());
    }

    private List<StatusPeriod> periods(long id, Set<ResidentStatus> kinds, Instant from, Instant to) {
        return statusApi.statusPeriods(id, kinds, from, to);
    }

    @Test
    void T049_wideRange_returnsEveryAbsence_theOpenOneWithoutEnd() {
        long id = residentWithHistory();

        assertThat(periods(id, ABSENT_KINDS, T0, END))
                .containsExactly(
                        new StatusPeriod(ResidentStatus.TEMPORARILY_ABSENT, T1, Optional.of(T2)),
                        new StatusPeriod(ResidentStatus.HOSPITALIZED, T3, Optional.empty()));
    }

    @Test
    void T049_onlyTheAskedStatuses_areReturned() {
        long id = residentWithHistory();

        assertThat(periods(id, Set.of(ResidentStatus.HOSPITALIZED), T0, END))
                .containsExactly(new StatusPeriod(ResidentStatus.HOSPITALIZED, T3, Optional.empty()));
        assertThat(periods(id, Set.of(ResidentStatus.OFF_SITE_ACTIVITY), T0, END))
                .isEmpty();
        assertThat(periods(id, Set.of(), T0, END)).isEmpty();
    }

    @Test
    void T049_range_returnsOnlyPeriodsThatOverlapIt_withoutCuttingThem() {
        long id = residentWithHistory();

        // nằm trong lần Tạm vắng: trả nguyên khoảng T1 → T2
        assertThat(periods(id, ABSENT_KINDS, T1.plusSeconds(60), T1.plusSeconds(120)))
                .containsExactly(new StatusPeriod(ResidentStatus.TEMPORARILY_ABSENT, T1, Optional.of(T2)));
        // giữa hai lần vắng
        assertThat(periods(id, ABSENT_KINDS, T2.plusSeconds(60), T3.minusSeconds(60)))
                .isEmpty();
        // bắt đầu giữa lần điều trị đang diễn ra
        assertThat(periods(id, ABSENT_KINDS, T3.plusSeconds(3600), END))
                .containsExactly(new StatusPeriod(ResidentStatus.HOSPITALIZED, T3, Optional.empty()));
        // trước mọi lần vắng
        assertThat(periods(id, ABSENT_KINDS, T0, T1.minusSeconds(1))).isEmpty();
    }

    @Test
    void T049_aPeriodThatEndsExactlyWhenTheRangeStarts_doesNotOverlap() {
        long id = residentWithHistory();

        assertThat(periods(id, ABSENT_KINDS, T2, T3.minusSeconds(1))).isEmpty();
    }

    @Test
    void T049_otherResidentsHistory_isNotMixedIn() {
        long id = residentWithHistory();
        long other = admissionResident();
        row(other, "ADMISSION_IN_PROGRESS", "IN_RESIDENCE", T0);

        assertThat(periods(other, ABSENT_KINDS, T0, END)).isEmpty();
        assertThat(periods(id, ABSENT_KINDS, T0, END)).hasSize(2);
    }
}
