package com.cambae.backend.m02.internal.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m02.internal.registration.service.ExpectedStayDuration.StayPeriod;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 018 T003 (FR-001, FR-001a, FR-007; BR-M02-11, Q-215, Q-241, Q-336; research Z-03): tổng thời gian lưu trú dự kiến tính
 * từ ngày bắt đầu hợp đồng đầu của chuỗi nối tiếp không gián đoạn (hoặc ngày hiệu lực phần nội trú) tới ngày kết thúc dự
 * kiến mới nhất, gồm phụ lục gia hạn; đếm cả ngày đầu và ngày cuối (chủ dự án chốt 2026-10-27).
 */
class ExpectedStayDurationTest {

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static StayPeriod period(String start, String end) {
        return new StayPeriod(d(start), end == null ? null : d(end));
    }

    @Test
    void initialContract_countsBothEnds() {
        var stay = ExpectedStayDuration.of(List.of(period("2026-10-01", "2027-09-30")), null);

        assertThat(stay.from()).isEqualTo(d("2026-10-01"));
        assertThat(stay.to()).isEqualTo(d("2027-09-30"));
        assertThat(stay.days()).hasValue(365);
    }

    @Test
    void shortStayOf14Days_isFourteenDays() {
        assertThat(ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-14")), null)
                        .days())
                .hasValue(14);
    }

    @Test
    void oneDayContract_isOneDay() {
        assertThat(ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-01")), null)
                        .days())
                .hasValue(1);
    }

    /** Phụ lục gia hạn đổi ngày kết thúc dự kiến của hợp đồng; bên gọi truyền ngày đã gồm gia hạn (US3 #6: 14 + 7). */
    @Test
    void contractExtendedByAddendum_usesTheExtendedEnd() {
        assertThat(ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-21")), null)
                        .days())
                .hasValue(21);
    }

    @Test
    void contractWithoutEndDate_isUnbounded() {
        var stay = ExpectedStayDuration.of(List.of(period("2026-10-01", null)), null);

        assertThat(stay.to()).isNull();
        assertThat(stay.days()).isEmpty();
    }

    /** Q-241: hợp đồng sau bắt đầu ngay ngày sau ngày kết thúc hợp đồng trước thì cộng dồn. */
    @Test
    void contiguousContracts_areAddedUp() {
        var stay = ExpectedStayDuration.of(
                List.of(period("2026-10-01", "2026-10-20"), period("2026-10-21", "2026-11-09")), null);

        assertThat(stay.from()).isEqualTo(d("2026-10-01"));
        assertThat(stay.days()).hasValue(40);
    }

    @Test
    void contiguousChain_endingWithoutEndDate_isUnbounded() {
        var stay =
                ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-20"), period("2026-10-21", null)), null);

        assertThat(stay.from()).isEqualTo(d("2026-10-01"));
        assertThat(stay.days()).isEmpty();
    }

    /** Q-241, quickstart Q20: khoảng trống 1 ngày thì tính lại từ hợp đồng sau. */
    @Test
    void oneDayGap_restartsFromTheLaterContract() {
        var stay = ExpectedStayDuration.of(
                List.of(period("2026-10-01", "2026-10-20"), period("2026-10-22", "2026-11-05")), null);

        assertThat(stay.from()).isEqualTo(d("2026-10-22"));
        assertThat(stay.days()).hasValue(15);
    }

    @Test
    void chainAfterAGap_addsUpOnlyItsOwnContracts() {
        var stay = ExpectedStayDuration.of(
                List.of(
                        period("2026-11-01", "2026-11-10"),
                        period("2026-09-01", "2026-09-30"),
                        period("2026-11-11", "2026-11-30")),
                null);

        assertThat(stay.from()).isEqualTo(d("2026-11-01"));
        assertThat(stay.to()).isEqualTo(d("2026-11-30"));
        assertThat(stay.days()).hasValue(30);
    }

    /** FR-001a, Q-336: bán trú chuyển nội trú bằng phụ lục; tính từ ngày hiệu lực phần nội trú, kể cả khi ở tương lai. */
    @Test
    void residentialPartEffectiveDate_startsTheCount() {
        var stay = ExpectedStayDuration.of(List.of(period("2026-06-01", "2026-12-31")), d("2026-12-10"));

        assertThat(stay.from()).isEqualTo(d("2026-12-10"));
        assertThat(stay.days()).hasValue(22);
    }

    /** FR-001a: hợp đồng nội trú mới nối tiếp hợp đồng bán trú; tính từ ngày bắt đầu hợp đồng nội trú. */
    @Test
    void newResidentialContractAfterDayCare_startsAtItsStart() {
        var stay = ExpectedStayDuration.of(
                List.of(period("2026-06-01", "2026-09-30"), period("2026-10-01", "2026-10-29")), d("2026-10-01"));

        assertThat(stay.from()).isEqualTo(d("2026-10-01"));
        assertThat(stay.days()).hasValue(29);
    }

    @Test
    void residentialDateBeforeTheChain_keepsTheChainStart() {
        var stay = ExpectedStayDuration.of(
                List.of(period("2026-06-01", "2026-06-30"), period("2026-08-01", "2026-08-31")), d("2026-06-15"));

        assertThat(stay.from()).isEqualTo(d("2026-08-01"));
        assertThat(stay.days()).hasValue(31);
    }

    @Test
    void invalidInput_isRejected() {
        assertThatThrownBy(() -> ExpectedStayDuration.of(List.of(), null))
                .as("không có hợp đồng")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpectedStayDuration.of(List.of(period("2026-10-10", "2026-10-09")), null))
                .as("kết thúc trước bắt đầu")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StayPeriod(null, d("2026-10-09")))
                .as("thiếu ngày bắt đầu")
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ExpectedStayDuration.of(List.of(period("2026-10-01", "2026-10-31")), d("2026-11-01")))
                .as("phần nội trú hiệu lực sau ngày kết thúc")
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** DBR-06 (004) không cho hợp đồng chồng nhau; dữ liệu chồng là sai, không tự hợp nhất. */
    @Test
    void overlappingContracts_areRejected() {
        assertThatThrownBy(() -> ExpectedStayDuration.of(
                        List.of(period("2026-10-01", "2026-10-20"), period("2026-10-20", "2026-11-05")), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpectedStayDuration.of(
                        List.of(period("2026-10-01", null), period("2026-12-01", "2026-12-31")), null))
                .as("hợp đồng không ngày kết thúc rồi hợp đồng khác")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
