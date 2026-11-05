package com.eldercare.backend.support.fake.family;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.dto.IdDocumentView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả {@link FamilyRightsApi} (012 cài) cho 013, 014, 017, 018: quan hệ Hiệu lực theo thứ tự thêm, không xét thời
 * điểm; quyền có tác dụng là quyền đang bật (test tự bỏ quyền xem sức khỏe khi muốn giả bản đồng ý không bao gồm). Thay
 * bản thật cho cả context nên test cần thì {@code @Import(FakeFamilyRights.Config.class)}.
 */
public class FakeFamilyRights implements FamilyRightsApi {

    private final List<Member> members = new ArrayList<>();
    private final Map<Long, IdDocumentView> documents = new HashMap<>();

    /** Quan hệ Hiệu lực mới, loại {@code OTHER}, không vai trò, quyền đã cho. */
    public FakeFamilyRights relate(long familyMemberId, long residentId, FamilyRight... rights) {
        members.add(new Member(familyMemberId, residentId, false, false, rights));
        return this;
    }

    public FakeFamilyRights representative(long familyMemberId, long residentId, FamilyRight... rights) {
        members.add(new Member(familyMemberId, residentId, true, false, rights));
        return this;
    }

    public FakeFamilyRights primaryContact(long familyMemberId, long residentId, FamilyRight... rights) {
        members.add(new Member(familyMemberId, residentId, false, true, rights));
        return this;
    }

    public FakeFamilyRights idDocument(long familyMemberId, String type, String number) {
        documents.put(familyMemberId, new IdDocumentView(type, number));
        return this;
    }

    public void reset() {
        members.clear();
        documents.clear();
    }

    @Override
    public boolean hasActiveRelationship(long familyMemberId, long residentId) {
        return find(familyMemberId, residentId).isPresent();
    }

    @Override
    public boolean rightEffective(long familyMemberId, long residentId, FamilyRight right, Instant at) {
        return find(familyMemberId, residentId)
                .map(m -> m.rights.contains(right))
                .orElse(false);
    }

    @Override
    public boolean isRepresentative(long familyMemberId, long residentId, Instant at) {
        return find(familyMemberId, residentId).map(m -> m.representative).orElse(false);
    }

    @Override
    public boolean canViewBelongings(long familyMemberId, long residentId, Instant at) {
        return isRepresentative(familyMemberId, residentId, at)
                || rightEffective(familyMemberId, residentId, FamilyRight.PICKUP_ALLOWED, at);
    }

    @Override
    public List<FamilyMemberView> representatives(long residentId) {
        return ofResident(residentId).stream()
                .filter(m -> m.representative)
                .map(Member::view)
                .toList();
    }

    @Override
    public Optional<FamilyMemberView> primaryContact(long residentId) {
        return ofResident(residentId).stream()
                .filter(m -> m.primaryContact)
                .map(Member::view)
                .findFirst();
    }

    @Override
    public List<FamilyMemberView> allowedPickersAt(long residentId, Instant at) {
        return ofResident(residentId).stream()
                .filter(m -> m.rights.contains(FamilyRight.PICKUP_ALLOWED))
                .map(Member::view)
                .toList();
    }

    @Override
    public Optional<IdDocumentView> idDocument(long familyMemberId) {
        return Optional.ofNullable(documents.get(familyMemberId));
    }

    private Optional<Member> find(long familyMemberId, long residentId) {
        return members.stream()
                .filter(m -> m.familyMemberId == familyMemberId && m.residentId == residentId)
                .findFirst();
    }

    private List<Member> ofResident(long residentId) {
        return members.stream().filter(m -> m.residentId == residentId).toList();
    }

    private static final class Member {
        private final long familyMemberId;
        private final long residentId;
        private final boolean representative;
        private final boolean primaryContact;
        private final Set<FamilyRight> rights = EnumSet.noneOf(FamilyRight.class);

        Member(
                long familyMemberId,
                long residentId,
                boolean representative,
                boolean primaryContact,
                FamilyRight[] rights) {
            this.familyMemberId = familyMemberId;
            this.residentId = residentId;
            this.representative = representative;
            this.primaryContact = primaryContact;
            this.rights.addAll(List.of(rights));
        }

        FamilyMemberView view() {
            return new FamilyMemberView(
                    familyMemberId,
                    "Người thân " + familyMemberId,
                    "09" + familyMemberId,
                    FamilyRelationshipType.OTHER);
        }
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        FakeFamilyRights fakeFamilyRights() {
            return new FakeFamilyRights();
        }
    }
}
