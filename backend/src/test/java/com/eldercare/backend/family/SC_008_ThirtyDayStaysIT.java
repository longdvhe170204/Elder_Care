package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * 012 T039 (SC-008; quickstart mục 5, Q31, Q32; FR-041 → FR-043, BR-M10-04): bộ 30 ngày người thân ở lại so với bảng
 * tính tay. Lượt qua đêm, lượt trong ngày, lượt qua mốc đổi đơn giá (đơn giá do 010 tra theo {@code incurredOn}, nên 012
 * chỉ cần đúng ngày của từng đêm), lượt Chờ xác nhận và lượt Từ chối. Việc nửa đêm chạy ở mọi mốc 00:00 của 30 ngày; số
 * đêm tính phí từng lượt và ngày của từng đêm khớp bảng; lượt chưa xác nhận không Bắt đầu được. Phần số suất ăn chờ 011
 * khai {@code LuotOLaiDaDoi} (team-plan mục 8).
 */
class SC_008_ThirtyDayStaysIT extends StayTestBase {

    private static final String REJECT = "{\"reason\": \"Người cùng phòng không đồng ý\"}";

    @Test
    void SC_008_thirtyDays_nightsPerStayMatchHandTable_unconfirmedNeverStarts() throws Exception {
        // Lượt 1: qua đêm 3 đêm (đêm của ngày 0, 1, 2)
        long overnight = confirmed(rMember, at(day, 18, 0), at(day.plusDays(3), 7, 0));
        // Lượt 2: trong ngày, không qua 00:00 (mỗi người cao tuổi một lượt cùng lúc, CFG-M10-10)
        long daytime = confirmed(mMember, at(day.plusDays(5), 8, 0), at(day.plusDays(5), 17, 0));
        // Lượt 3: 5 đêm, qua mốc đổi đơn giá ngày 12 (010 tính đêm 10, 11 theo giá cũ, 12 → 14 theo giá mới)
        long acrossPriceChange = confirmed(rMember, at(day.plusDays(10), 19, 0), at(day.plusDays(15), 8, 0));
        // Lượt 4: 2 đêm
        long twoNights = confirmed(mMember, at(day.plusDays(20), 20, 0), at(day.plusDays(22), 6, 0));
        // Lượt 5: đăng ký nhưng chưa xác nhận; lượt 6: Trưởng tầng từ chối
        long pending = registered(rMember, at(day.plusDays(25), 18, 0), at(day.plusDays(27), 8, 0));
        long rejected = registered(mMember, at(day.plusDays(28), 18, 0), at(day.plusDays(29), 8, 0));
        command(staff(floorManager), rejected, "reject", REJECT).andExpect(status().isOk());

        Map<Instant, List<Runnable>> plan = new TreeMap<>();
        step(plan, at(day, 18, 0), overnight, "start");
        step(plan, at(day.plusDays(3), 7, 0), overnight, "end");
        step(plan, at(day.plusDays(5), 8, 0), daytime, "start");
        step(plan, at(day.plusDays(5), 17, 0), daytime, "end");
        step(plan, at(day.plusDays(10), 19, 0), acrossPriceChange, "start");
        step(plan, at(day.plusDays(15), 8, 0), acrossPriceChange, "end");
        step(plan, at(day.plusDays(20), 20, 0), twoNights, "start");
        step(plan, at(day.plusDays(22), 6, 0), twoNights, "end");
        plan.computeIfAbsent(at(day.plusDays(25), 18, 0), k -> new java.util.ArrayList<>())
                .add(() -> refused(pending));
        plan.computeIfAbsent(at(day.plusDays(28), 18, 0), k -> new java.util.ArrayList<>())
                .add(() -> refused(rejected));
        for (int d = 1; d <= 30; d++) {
            Instant midnight = at(day.plusDays(d), 0, 0);
            plan.computeIfAbsent(midnight, k -> new java.util.ArrayList<>()).add(() -> runNights(midnight));
        }
        for (Map.Entry<Instant, List<Runnable>> e : plan.entrySet()) {
            moveTo(e.getKey());
            e.getValue().forEach(Runnable::run);
        }

        // Bảng tính tay: lượt → ngày của từng đêm tính phí
        assertThat(nights(overnight)).containsExactly(day, day.plusDays(1), day.plusDays(2));
        assertThat(nights(daytime)).isEmpty();
        assertThat(nights(acrossPriceChange))
                .containsExactly(
                        day.plusDays(10), day.plusDays(11), day.plusDays(12), day.plusDays(13), day.plusDays(14));
        assertThat(nights(twoNights)).containsExactly(day.plusDays(20), day.plusDays(21));
        assertThat(nights(pending)).isEmpty();
        assertThat(nights(rejected)).isEmpty();
        assertThat(billed()).hasSize(10).allSatisfy(b -> {
            assertThat(b.sourceType()).isEqualTo("OVERNIGHT_STAY");
            assertThat(b.unit()).isEqualTo("NIGHT");
            assertThat(b.quantity()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(b.residentId()).isEqualTo(a);
        });

        assertThat(stayStatus(overnight)).isEqualTo("ENDED");
        assertThat(stayStatus(daytime)).isEqualTo("ENDED");
        assertThat(stayStatus(acrossPriceChange)).isEqualTo("ENDED");
        assertThat(stayStatus(twoNights)).isEqualTo("ENDED");
        assertThat(stayStatus(pending)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(stayStatus(rejected)).isEqualTo("REJECTED");
    }

    private void step(Map<Instant, List<Runnable>> plan, Instant at, long stayId, String command) {
        plan.computeIfAbsent(at, k -> new java.util.ArrayList<>()).add(() -> {
            try {
                command(office, stayId, command, "{}").andExpect(status().isOk());
            } catch (Exception e) {
                throw new AssertionError(command + " lượt " + stayId, e);
            }
        });
    }

    /** Lượt chưa Đã xác nhận: Bắt đầu bị chặn. */
    private void refused(long stayId) {
        try {
            command(office, stayId, "start", "{}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        } catch (Exception e) {
            throw new AssertionError("start lượt " + stayId, e);
        }
    }

    private List<CostSourceBilled> billed() {
        return events.stream(CostSourceBilled.class)
                .filter(e -> "OVERNIGHT_STAY".equals(e.sourceType()))
                .filter(e -> e.residentId() == a)
                .toList();
    }

    private List<LocalDate> nights(long stayId) {
        return billed().stream()
                .filter(e -> e.sourceId().startsWith(stayId + ":"))
                .map(CostSourceBilled::incurredOn)
                .sorted()
                .toList();
    }
}
