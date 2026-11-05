package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.notification.api.spi.FamilyContactView;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Bản cài {@code NguoiThanNhanThongBaoPort} của 009 (012 T006; contracts mục 1.1; 009 FR-029a). Người liên hệ chính
 * luôn có quyền nhận khẩn (Q-96). Tài khoản cổng Khóa tạm vẫn nhận thông báo (009 FR-008).
 */
@Service
class FamilyNotificationLookup implements FamilyNotificationPort {

    private static final Set<AccountStatus> RECEIVING =
            EnumSet.of(AccountStatus.ACTIVE, AccountStatus.TEMPORARILY_LOCKED);

    private final RelationshipQueries queries;
    private final AccountApi accounts;

    FamilyNotificationLookup(RelationshipQueries queries, AccountApi accounts) {
        this.queries = queries;
        this.accounts = accounts;
    }

    @Override
    public List<NotifiableRelationshipView> activeRelationships(long residentId) {
        return queries.activeOfResident(residentId).stream()
                .map(r -> {
                    Optional<AccountView> account = accounts.byFamilyMember(r.familyMemberId());
                    return new NotifiableRelationshipView(
                            r.familyMemberId(),
                            r.primaryContact(),
                            r.representative(),
                            rights(r),
                            account.map(AccountView::accountId),
                            receiving(account),
                            queries.familyMember(r.familyMemberId()).map(FamilyMember::phoneNumber));
                })
                .toList();
    }

    /** Người đại diện, rồi người có quyền nhận khẩn; trong mỗi nhóm theo thứ tự liên hệ (FR-012). */
    @Override
    public List<Long> callOrder(long residentId) {
        List<FamilyRelationship> active = queries.activeOfResident(residentId);
        return Stream.concat(
                        active.stream().filter(FamilyRelationship::representative),
                        active.stream()
                                .filter(r -> !r.representative())
                                .filter(r -> rights(r).contains(FamilyRight.RECEIVE_URGENT)))
                .map(FamilyRelationship::familyMemberId)
                .distinct()
                .toList();
    }

    @Override
    public Optional<FamilyContactView> contact(long familyMemberId) {
        return queries.familyMember(familyMemberId).map(m -> {
            Optional<AccountView> account = accounts.byFamilyMember(familyMemberId);
            return new FamilyContactView(
                    m.id(),
                    m.fullName(),
                    Optional.of(m.phoneNumber()),
                    account.map(AccountView::accountId),
                    receiving(account));
        });
    }

    private static Set<FamilyRight> rights(FamilyRelationship r) {
        Set<FamilyRight> rights = EnumSet.noneOf(FamilyRight.class);
        rights.addAll(r.enabledRights());
        if (r.primaryContact()) {
            rights.add(FamilyRight.RECEIVE_URGENT);
        }
        return rights;
    }

    private static boolean receiving(Optional<AccountView> account) {
        return account.map(a -> RECEIVING.contains(a.status())).orElse(false);
    }
}
