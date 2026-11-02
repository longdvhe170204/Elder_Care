package com.eldercare.backend.support.fake.administration;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipView;
import com.eldercare.backend.common.notification.FamilyRight;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Bản giả {@link FamilyRelationshipApi} (012 cài): quan hệ Hiệu lực test đặt sẵn, không xét thời điểm. */
public class FakeFamilyRelationships implements FamilyRelationshipApi {

    private final java.util.Map<Long, String> names = new java.util.HashMap<>();

    private final List<FamilyRelationshipView> relationships = new ArrayList<>();
    private final Map<Long, Long> familyMemberOf = new HashMap<>();
    private final Set<Long> representatives = new HashSet<>();
    private final Map<Long, String> phoneNumbers = new HashMap<>();
    private final Map<Long, FamilyRelationshipType> types = new HashMap<>();
    private final Set<String> disabledRights = new HashSet<>();
    private long nextId = 1;

    /** Quan hệ Hiệu lực mới, loại {@code OTHER}; trả id quan hệ. */
    public long relate(long familyMemberId, long residentId) {
        return relate(familyMemberId, residentId, FamilyRelationshipType.OTHER);
    }

    /** Quan hệ Hiệu lực mới có loại; trả id quan hệ. */
    public long relate(long familyMemberId, long residentId, FamilyRelationshipType type) {
        long id = nextId++;
        relationships.add(new FamilyRelationshipView(id, residentId));
        familyMemberOf.put(id, familyMemberId);
        types.put(id, type);
        return id;
    }

    /** Kết thúc quan hệ. */
    public FakeFamilyRelationships end(long relationshipId) {
        relationships.removeIf(r -> r.relationshipId() == relationshipId);
        return this;
    }

    public FakeFamilyRelationships representative(long relationshipId) {
        representatives.add(relationshipId);
        return this;
    }

    public FakeFamilyRelationships phoneNumber(long familyMemberId, String phoneNumber) {
        phoneNumbers.put(familyMemberId, phoneNumber);
        return this;
    }

    @Override
    public List<FamilyRelationshipView> active(long familyMemberId) {
        return relationships.stream()
                .filter(r -> familyMemberOf.get(r.relationshipId()) == familyMemberId)
                .toList();
    }

    @Override
    public Optional<String> registeredPhoneNumber(long familyMemberId) {
        return Optional.ofNullable(phoneNumbers.get(familyMemberId));
    }

    @Override
    public boolean isActiveRepresentative(long familyMemberId, long residentId, Instant at) {
        return active(familyMemberId).stream()
                .anyMatch(r -> r.residentId() == residentId && representatives.contains(r.relationshipId()));
    }

    /** Tắt một quyền riêng của người thân với người cao tuổi; mặc định mọi quyền đang bật. */
    public FakeFamilyRelationships disable(long familyMemberId, long residentId, FamilyRight right) {
        disabledRights.add(familyMemberId + ":" + residentId + ":" + right);
        return this;
    }

    @Override
    public boolean rightEnabled(long familyMemberId, long residentId, FamilyRight right, Instant at) {
        return !disabledRights.contains(familyMemberId + ":" + residentId + ":" + right);
    }

    public FakeFamilyRelationships name(long familyMemberId, String fullName) {
        names.put(familyMemberId, fullName);
        return this;
    }

    @Override
    public Optional<String> familyMemberName(long familyMemberId) {
        return Optional.ofNullable(names.get(familyMemberId));
    }

    @Override
    public boolean belongsToResident(long familyMemberId, long residentId) {
        return active(familyMemberId).stream().anyMatch(r -> r.residentId() == residentId);
    }

    @Override
    public Optional<FamilyRelationshipType> relationship(long familyMemberId, long residentId) {
        return active(familyMemberId).stream()
                .filter(r -> r.residentId() == residentId)
                .map(r -> types.get(r.relationshipId()))
                .findFirst();
    }
}
