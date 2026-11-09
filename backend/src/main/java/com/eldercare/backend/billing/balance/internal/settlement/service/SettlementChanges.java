package com.eldercare.backend.billing.balance.internal.settlement.service;

import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.ResidencyEndingReadApi;
import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.api.dto.SettlementInfoView;
import com.eldercare.backend.admission.api.events.DeathRecorded;
import com.eldercare.backend.admission.api.events.EndingExceptionApproved;
import com.eldercare.backend.admission.api.events.ResidencyEndingCancelled;
import com.eldercare.backend.admission.api.events.ResidencyEndingCreated;
import com.eldercare.backend.admission.api.events.SettlementConditionChanged;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.service.PaymentService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo 004 khi điều kiện quyết toán có thể đã đổi (017 T031; FR-022, FR-023; module-interfaces mục 2.4, 3.1): sau mỗi giao
 * dịch, Chờ duyệt mới hoặc đóng, xác nhận quyết toán, và khi 004 lập, hủy hồ sơ kết thúc, ghi qua đời, duyệt ngoại lệ.
 * Chỉ phát khi hồ sơ đang có căn cứ quyết toán (hồ sơ kết thúc Đang chuẩn bị, danh sách việc sau qua đời đang mở); 004
 * đọc lại hai mã qua bean điều kiện nên phát khi kết quả không đổi cũng không sai. Danh sách việc sau qua đời được tính lại
 * ngay ({@link PostDeathTaskApi#refresh}) để đóng khi mục quyết toán vừa Hoàn thành.
 */
@Component
public class SettlementChanges implements PaymentService.PaymentRecordedHook {

    private final ObjectProvider<ResidencyEndingReadApi> endings;
    private final ObjectProvider<PostDeathTaskApi> postDeathTasks;
    private final ApplicationEventPublisher events;

    SettlementChanges(
            ObjectProvider<ResidencyEndingReadApi> endings,
            ObjectProvider<PostDeathTaskApi> postDeathTasks,
            ApplicationEventPublisher events) {
        this.endings = endings;
        this.postDeathTasks = postDeathTasks;
        this.events = events;
    }

    /** Gọi trong transaction của lệnh vừa đổi sổ hoặc giao dịch Chờ duyệt của hồ sơ. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void changed(long residentId) {
        publish(residentId)
                .filter(info -> info.endCase() == EndCase.DEATH)
                .ifPresent(info -> postDeathTasks.ifAvailable(api -> api.refresh(residentId)));
    }

    @Override
    public void paymentRecorded(long residentId, List<BalanceTransactionRow> rows) {
        changed(residentId);
    }

    // Sự kiện của 010, 004: chỉ báo lại; danh sách việc sau qua đời do module phát sự kiện tự tính lại trong lệnh của
    // mình

    @EventListener
    public void on(CostStatementFinalized event) {
        publish(event.residentId());
    }

    @EventListener
    public void on(ResidencyEndingCreated event) {
        publish(event.residentId());
    }

    @EventListener
    public void on(ResidencyEndingCancelled event) {
        // hồ sơ đã hủy, không còn căn cứ: vẫn báo để 004 làm mới
        events.publishEvent(new SettlementConditionChanged(event.residentId()));
    }

    @EventListener
    public void on(DeathRecorded event) {
        publish(event.residentId());
    }

    @EventListener
    public void on(EndingExceptionApproved event) {
        publish(event.residentId());
    }

    private Optional<SettlementInfoView> publish(long residentId) {
        ResidencyEndingReadApi ending = endings.getIfAvailable();
        Optional<SettlementInfoView> info = ending == null ? Optional.empty() : ending.settlementInfo(residentId);
        info.ifPresent(i -> events.publishEvent(new SettlementConditionChanged(residentId)));
        return info;
    }
}
