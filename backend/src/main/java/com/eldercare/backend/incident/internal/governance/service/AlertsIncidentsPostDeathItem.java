package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.domain.ResidentItemClosed;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Mục {@code ALERTS_INCIDENTS} của danh sách việc sau qua đời (tên logic {@code MucViecSauQuaDoiPort}
 * {@code CANH_BAO_SU_CO}; research S-17, 004 FR-071): Chưa hoàn thành khi còn cảnh báo đang mở hoặc sự cố mở; Không áp
 * dụng khi người đó chưa từng có; còn lại Hoàn thành. Chỉ đọc, không khóa. Sau mỗi lần đóng hoặc hủy cảnh báo, sự cố, gọi
 * {@link PostDeathTaskApi#refresh} trong cùng transaction (004 chỉ tính lại khi đã có ghi nhận qua đời).
 */
@Service
public class AlertsIncidentsPostDeathItem implements PostDeathItemPort {
    private static final String BASIS = "M05: cảnh báo, sự cố của người cao tuổi";

    private final AlertStore alerts;
    private final IncidentStore incidents;
    private final ObjectProvider<PostDeathTaskApi> postDeathTasks;

    public AlertsIncidentsPostDeathItem(
            AlertStore alerts, IncidentStore incidents, ObjectProvider<PostDeathTaskApi> postDeathTasks) {
        this.alerts = alerts;
        this.incidents = incidents;
        this.postDeathTasks = postDeathTasks;
    }

    @Override
    public PostDeathItemCode code() {
        return PostDeathItemCode.ALERTS_INCIDENTS;
    }

    @Override
    public PostDeathItemStatusView status(long residentId) {
        if (alerts.hasOpenAlert(residentId) || incidents.hasOpen(residentId)) {
            return new PostDeathItemStatusView(PostDeathItemStatus.NOT_COMPLETED, BASIS);
        }
        if (!alerts.hasAnyAlert(residentId) && !incidents.hasAny(residentId)) {
            return new PostDeathItemStatusView(PostDeathItemStatus.NOT_APPLICABLE, BASIS);
        }
        return new PostDeathItemStatusView(PostDeathItemStatus.COMPLETED, BASIS);
    }

    @EventListener
    public void on(ResidentItemClosed event) {
        postDeathTasks.ifAvailable(api -> api.refresh(event.residentId()));
    }
}
