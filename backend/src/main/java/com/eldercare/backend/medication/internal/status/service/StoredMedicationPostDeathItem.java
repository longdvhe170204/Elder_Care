package com.eldercare.backend.medication.internal.status.service;

import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.status.domain.StoredMedicationClosed;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Mục {@code STORED_MEDICATION} của danh sách việc sau qua đời (tên logic {@code MucViecSauQuaDoiPort} {@code THUOC_GUI};
 * 006 contracts/module-interfaces mục 1, research D-14; 004 FR-071, Q-64): Chưa hoàn thành khi còn lô Chờ đối chiếu, Được
 * sử dụng, Chỉ giữ hộ hoặc lần giao thuốc mang theo chưa Đã nhận lại (cùng điều kiện {@code KT_C_THUOC_GUI}); Không áp dụng
 * khi chưa từng có lô, lần giao nào; còn lại Hoàn thành. Chỉ đọc, không khóa. Sau mỗi lần hoàn trả, hủy lô hoặc nhận lại
 * lần giao, gọi {@link PostDeathTaskApi#refresh} trong cùng transaction (004 chỉ tính lại khi đã có ghi nhận qua đời).
 */
@Service
public class StoredMedicationPostDeathItem implements PostDeathItemPort {
    private static final String BASIS = "M07: thuốc gia đình gửi, thuốc mang theo của người cao tuổi";
    private static final List<FamilyMedicationBatchStatus> HELD_BATCHES = List.of(
            FamilyMedicationBatchStatus.PENDING_RECONCILIATION,
            FamilyMedicationBatchStatus.IN_USE,
            FamilyMedicationBatchStatus.HOLD_ONLY);
    private static final List<HandoverStatus> OPEN_HANDOVERS =
            List.of(HandoverStatus.HANDED_OVER, HandoverStatus.WAITING_RETURN);

    private final FamilyMedicationBatchRepository batches;
    private final MedicationHandoverRepository handovers;
    private final ObjectProvider<PostDeathTaskApi> postDeathTasks;

    public StoredMedicationPostDeathItem(
            FamilyMedicationBatchRepository batches,
            MedicationHandoverRepository handovers,
            ObjectProvider<PostDeathTaskApi> postDeathTasks) {
        this.batches = batches;
        this.handovers = handovers;
        this.postDeathTasks = postDeathTasks;
    }

    @Override
    public PostDeathItemCode code() {
        return PostDeathItemCode.STORED_MEDICATION;
    }

    @Override
    public PostDeathItemStatusView status(long residentId) {
        if (!batches.findByResidentIdAndStatusIn(residentId, HELD_BATCHES).isEmpty()
                || !handovers
                        .findByResidentIdAndStatusIn(residentId, OPEN_HANDOVERS)
                        .isEmpty()) {
            return new PostDeathItemStatusView(PostDeathItemStatus.NOT_COMPLETED, BASIS);
        }
        if (batches.findByResidentId(residentId).isEmpty()
                && handovers.findByResidentId(residentId).isEmpty()) {
            return new PostDeathItemStatusView(PostDeathItemStatus.NOT_APPLICABLE, BASIS);
        }
        return new PostDeathItemStatusView(PostDeathItemStatus.COMPLETED, BASIS);
    }

    @EventListener
    public void on(StoredMedicationClosed event) {
        postDeathTasks.ifAvailable(api -> api.refresh(event.residentId()));
    }
}
