package com.cambae.backend.support.fake.m13;

import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.FamilyNotificationPort;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bản giả {@link FamilyNotificationPort} (012 cài; 009 T003): quan hệ, thứ tự gọi và liên hệ do test đặt sẵn. Không có
 * dữ liệu thì trả rỗng như khi 012 chưa có bean (contracts mục 2.1).
 */
public class FakeFamilyNotifications implements FamilyNotificationPort {

    private final Map<Long, List<NotifiableRelationshipView>> relationships = new HashMap<>();
    private final Map<Long, List<Long>> callOrders = new HashMap<>();
    private final Map<Long, FamilyContactView> contacts = new HashMap<>();

    /** Thêm một quan hệ Hiệu lực của người thân với người cao tuổi. */
    public FakeFamilyNotifications relate(long residentId, NotifiableRelationshipView relationship) {
        relationships.computeIfAbsent(residentId, k -> new ArrayList<>()).add(relationship);
        return this;
    }

    public FakeFamilyNotifications callOrder(long residentId, List<Long> familyMemberIds) {
        callOrders.put(residentId, List.copyOf(familyMemberIds));
        return this;
    }

    public FakeFamilyNotifications contact(FamilyContactView contact) {
        contacts.put(contact.familyMemberId(), contact);
        return this;
    }

    /** Xóa mọi dữ liệu đã đặt (context test dùng chung giữa các lớp). */
    public void reset() {
        relationships.clear();
        callOrders.clear();
        contacts.clear();
    }

    @Override
    public List<NotifiableRelationshipView> activeRelationships(long residentId) {
        return List.copyOf(relationships.getOrDefault(residentId, List.of()));
    }

    @Override
    public List<Long> callOrder(long residentId) {
        return callOrders.getOrDefault(residentId, List.of());
    }

    @Override
    public Optional<FamilyContactView> contact(long familyMemberId) {
        return Optional.ofNullable(contacts.get(familyMemberId));
    }
}
