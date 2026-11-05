package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.family.internal.pickup.service.PickupService;
import com.eldercare.backend.family.internal.relationship.domain.Dbr02Check;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
import com.eldercare.backend.family.internal.right.service.RightChangeRequestService;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bean {@code ThamGiaChuyenTrangThai} của 012 (T012; contracts mục 1.1, FR-003): điều kiện {@code DBR_02} không miễn được
 * cho Hoàn tất tiếp nhận. Thứ tự sau 010, trước 013. Trạng thái cuối hủy yêu cầu thay đổi quyền Chờ xác nhận, quan hệ giữ
 * Hiệu lực (FR-075); yêu cầu ngoại lệ đón Chờ xác nhận cũng Hủy. Hủy lượt thăm, nhắc kết thúc lượt ở lại theo sự kiện
 * {@code ResidentStatusChanged} ({@code VisitAutomation}, {@code StayEndReminders}).
 */
@Component
@Order(70)
class M10StatusParticipant implements StatusTransitionParticipant {

    private final FamilyRelationshipStore store;
    private final RightChangeRequestService requests;
    private final PickupService pickups;

    M10StatusParticipant(FamilyRelationshipStore store, RightChangeRequestService requests, PickupService pickups) {
        this.store = store;
        this.requests = requests;
        this.pickups = pickups;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(
                StatusCommand.COMPLETE_ADMISSION,
                StatusCommand.CANCEL_ADMISSION,
                StatusCommand.TRANSFER_OUT,
                StatusCommand.END_RESIDENCY,
                StatusCommand.RECORD_DEATH);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.COMPLETE_ADMISSION
                || Dbr02Check.satisfied(FamilyMemberService.roles(store.activeOfResident(context.residentId())))) {
            return List.of();
        }
        return List.of(new Condition(
                "DBR_02", "Cần đúng một người liên hệ chính và ít nhất một người đại diện", "DBR-02", false));
    }

    @Override
    public void applyEffects(TransitionResult result) {
        if (RightChangeRequestService.isFinal(result.to())) {
            requests.cancelOnFinalStatus(result.residentId());
            pickups.cancelOnFinalStatus(result.residentId());
        }
    }
}
