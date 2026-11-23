package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.belongings.internal.belonging.domain.HandoverParties;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.staffing.api.dto.StaffPublicView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Tên người giao, người nhận trong lịch sử bàn giao trên màn nhân viên (FR-011, FR-020; test luồng BF-14 2026-11-23):
 * bản ghi chỉ lưu tên với người ngoài, với nhân viên, người thân, người cao tuổi chỉ lưu mã nên tra tên khi đọc.
 */
@Component
class HandoverPartyNames {

    private final BelongingHandoverStore handovers;
    private final ShiftApi shifts;
    private final FamilyRightsApi family;
    private final ResidentApi residents;

    HandoverPartyNames(
            BelongingHandoverStore handovers, ShiftApi shifts, FamilyRightsApi family, ResidentApi residents) {
        this.handovers = handovers;
        this.shifts = shifts;
        this.family = family;
        this.residents = residents;
    }

    /** Người cao tuổi giao, nhận không lưu mã trên bản ghi: là chủ đồ {@code residentId}. */
    List<HandoverView> named(long intakeHandoverId, long residentId, List<HandoverView> views) {
        Map<Long, HandoverParties> parties = handovers.partiesOfIntake(intakeHandoverId);
        Map<String, Optional<String>> cache = new HashMap<>();
        return views.stream()
                .map(v -> {
                    HandoverParties p = parties.get(v.id());
                    if (p == null) {
                        return v;
                    }
                    String giver =
                            v.giverName() != null ? v.giverName() : name(p.giverType(), p.giverId(), residentId, cache);
                    String recipient = v.recipientName() != null
                            ? v.recipientName()
                            : name(p.recipientType(), p.recipientId(), residentId, cache);
                    return v.withNames(giver, recipient);
                })
                .toList();
    }

    private String name(String type, Long id, long residentId, Map<String, Optional<String>> cache) {
        if ("RESIDENT".equals(type) && id == null) {
            id = residentId;
        }
        if (type == null || id == null) {
            return null;
        }
        long key = id;
        return cache.computeIfAbsent(type + ":" + key, k -> lookup(type, key)).orElse(null);
    }

    private Optional<String> lookup(String type, long id) {
        return switch (type) {
            case "STAFF" -> shifts.staff(id).map(StaffPublicView::fullName);
            case "FAMILY_MEMBER" -> family.fullName(id);
            case "RESIDENT" -> Optional.ofNullable(residents.identity(id).fullName());
            default -> Optional.empty();
        };
    }
}
