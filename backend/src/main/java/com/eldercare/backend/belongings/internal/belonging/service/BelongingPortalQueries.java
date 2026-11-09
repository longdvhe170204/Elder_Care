package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.consent.domain.SelfCustodyConsent;
import com.eldercare.backend.belongings.internal.consent.service.SelfCustodyConsentService;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồ gửi và lịch sử bàn giao trên cổng người thân (013 T028; FR-030, BR-M12-09, Q-151, research K-12): chỉ người đại diện
 * và người có quyền "được phép đón" ({@code FamilyRightsApi.canViewBelongings}), kiểm lại ở mỗi lần gọi. Không dùng
 * {@code @RequiresPermission} vì phạm vi "quan hệ" của 002 đọc bảng thật của 012; service kiểm tài khoản người thân.
 */
@Service
public class BelongingPortalQueries {

    /** Một đồ kèm lịch sử bàn giao của riêng đồ đó. */
    public record Item(
            BelongingView belonging, List<HandoverView> history, SimpleRecord<SelfCustodyConsent> selfCustodyConsent) {}

    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final SelfCustodyConsentService consents;
    private final FamilyRightsApi family;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    BelongingPortalQueries(
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            SelfCustodyConsentService consents,
            FamilyRightsApi family,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.belongings = belongings;
        this.handovers = handovers;
        this.consents = consents;
        this.family = family;
        this.time = time;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Item> view(long residentId) {
        CurrentUser user = users.getObject();
        if (user.type() != AccountType.FAMILY_MEMBER || user.familyMemberId().isEmpty()) {
            throw new PermissionDeniedException("Chỉ tài khoản người thân dùng cổng");
        }
        if (!family.canViewBelongings(user.familyMemberId().get(), residentId, time.now())) {
            throw new PermissionDeniedException("Chỉ người đại diện và người được phép đón xem đồ gửi (BR-M12-09)");
        }
        Map<Long, List<HandoverView>> byIntake = new HashMap<>();
        var effectiveConsents = consents.effectiveOfResident(residentId);
        return belongings.ofResident(residentId, null).stream()
                .map(b -> {
                    long intake = belongings.intakeHandoverOf(b.belongingId()).orElse(-1L);
                    List<HandoverView> own = byIntake.computeIfAbsent(intake, handovers::ofIntake).stream()
                            .filter(h -> h.belongingId() == b.belongingId())
                            .toList();
                    return new Item(b, own, effectiveConsents.get(b.belongingId()));
                })
                .toList();
    }
}
