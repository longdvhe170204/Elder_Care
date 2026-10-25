package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.internal.transition.service.M02DailyTask;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreCause;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import java.time.LocalDate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code CALCULATE_SCORES} của job hằng ngày (FR-010, research T-07, T-11): tính lại điểm của mọi hồ sơ Đang chờ và Đã
 * liên hệ. Điểm chỉ có nghĩa ở ngày mới nhất nên ngày bị lỡ ghi "bỏ qua" (BR-M02-19); mỗi hồ sơ khóa dòng riêng nên không
 * giữ khóa lâu. Lịch sử chỉ thêm khi điểm đổi.
 */
@Component
@Order(10)
class CalculateScoresTask implements M02DailyTask {

    static final String NAME = "CALCULATE_SCORES";

    private final WaitingListStore store;
    private final ScoreRecalculation scores;
    private final BusinessTime time;

    CalculateScoresTask(WaitingListStore store, ScoreRecalculation scores, BusinessTime time) {
        this.store = store;
        this.scores = scores;
        this.time = time;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean latestDayOnly() {
        return true;
    }

    @Override
    public int run(LocalDate day) {
        PriorityScoreTable table = scores.table();
        int changed = 0;
        for (WaitingListEntry listed : store.listOpen(time.startOfDay(day.plusDays(1)))) {
            WaitingListEntry entry = store.lock(listed.id()).orElse(null);
            if (entry == null || !entry.status().isOpen()) {
                continue;
            }
            if (scores.rescore(entry, table, day, ScoreCause.DAILY) != entry) {
                changed++;
            }
        }
        return changed;
    }
}
