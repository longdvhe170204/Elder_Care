package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.family.internal.relationship.domain.Dbr02Check;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
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
 * cho Hoàn tất tiếp nhận. Thứ tự sau 010, trước 013. Tác động của Cho tạm vắng, Điều trị tại bệnh viện (hủy lượt thăm,
 * nhắc kết thúc lượt ở lại) và của trạng thái cuối (FR-075: hủy yêu cầu chờ, ngoại lệ đón, lượt thăm tương lai; quan hệ
 * giữ Hiệu lực) thêm cùng US4, US8, US3, US2 khi có các bản ghi đó.
 */
@Component
@Order(70)
class M10StatusParticipant implements StatusTransitionParticipant {

    private final FamilyRelationshipStore store;

    M10StatusParticipant(FamilyRelationshipStore store) {
        this.store = store;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.COMPLETE_ADMISSION);
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
    public void applyEffects(TransitionResult result) {}
}
