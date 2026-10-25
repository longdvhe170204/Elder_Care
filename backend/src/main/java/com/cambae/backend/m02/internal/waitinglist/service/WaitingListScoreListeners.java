package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.events.CareLevelChanged;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreCause;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.parameter.api.ParameterChanged;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Tính lại điểm ngay khi dữ liệu nguồn đổi (FR-008, FR-010, research T-07): mức chăm sóc hiện hành của 001 đổi (hồ sơ nhận
 * mức mới, hết "chưa đánh giá"), bảng CFG-M02-08 đổi (mọi hồ sơ chưa ở trạng thái cuối). Chạy sau khi lệnh nguồn đã commit
 * (một giao dịch riêng); lỗi ở đây không làm hỏng lệnh nguồn, và việc hằng ngày sẽ tính lại ở ngày kế tiếp.
 */
@Component
class WaitingListScoreListeners {

    private final WaitingListStore store;
    private final ScoreRecalculation scores;
    private final CareLevelApi careLevels;
    private final ResidencyLock residencyLock;
    private final BusinessTime time;

    WaitingListScoreListeners(
            WaitingListStore store,
            ScoreRecalculation scores,
            CareLevelApi careLevels,
            ResidencyLock residencyLock,
            BusinessTime time) {
        this.store = store;
        this.scores = scores;
        this.careLevels = careLevels;
        this.residencyLock = residencyLock;
        this.time = time;
    }

    @ApplicationModuleListener
    void onCareLevelChanged(CareLevelChanged event) {
        residencyLock.lock(event.residentId());
        store.lockOpenByResident(event.residentId())
                .ifPresent(entry -> careLevels.current(event.residentId()).ifPresent(level -> {
                    store.updateCareLevel(entry.id(), level, false);
                    scores.rescore(store.find(entry.id()).orElseThrow(), time.today(), ScoreCause.CARE_LEVEL_CHANGED);
                }));
    }

    @ApplicationModuleListener
    void onParameterChanged(ParameterChanged event) {
        if (event.code() != ParameterCode.CFG_M02_08) {
            return;
        }
        PriorityScoreTable table = scores.currentTable();
        for (WaitingListEntry listed : store.listOpen(time.now().plusSeconds(1))) {
            store.lock(listed.id())
                    .filter(entry -> entry.status().isOpen())
                    .ifPresent(entry -> scores.rescore(entry, table, time.today(), ScoreCause.SCORE_TABLE_CHANGED));
        }
    }
}
