package com.cambae.backend.m13.internal.recipient.service;

import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.FamilyNotificationPort;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m15.account.api.spi.StaffProfileApi;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Số điện thoại hiện hành của người nhận cho tin nhắn (FR-016; contracts mục 2): nhân viên theo hồ sơ nhân viên (008,
 * {@code StaffProfileApi.phoneNumber}), người thân theo quan hệ với người cao tuổi hoặc liên hệ của người thân (012).
 * Người cao tuổi không nhận tin nhắn (FR-022a).
 */
@Service
public class RecipientPhones {

    private final ObjectProvider<StaffProfileApi> profiles;
    private final ObjectProvider<FamilyNotificationPort> family;

    RecipientPhones(ObjectProvider<StaffProfileApi> profiles, ObjectProvider<FamilyNotificationPort> family) {
        this.profiles = profiles;
        this.family = family;
    }

    public Optional<String> of(RecipientType type, long recipientId, Long residentId) {
        return switch (type) {
            case STAFF -> Optional.ofNullable(profiles.getIfAvailable()).flatMap(p -> p.phoneNumber(recipientId));
            case FAMILY_MEMBER -> familyPhone(recipientId, residentId);
            case RESIDENT -> Optional.empty();
        };
    }

    private Optional<String> familyPhone(long familyMemberId, Long residentId) {
        FamilyNotificationPort port = family.getIfAvailable();
        if (port == null) {
            return Optional.empty();
        }
        Optional<String> fromRelation = residentId == null
                ? Optional.empty()
                : port.activeRelationships(residentId).stream()
                        .filter(r -> r.familyMemberId() == familyMemberId)
                        .findFirst()
                        .flatMap(NotifiableRelationshipView::phoneNumber);
        return fromRelation.or(() -> port.contact(familyMemberId).flatMap(FamilyContactView::phoneNumber));
    }
}
