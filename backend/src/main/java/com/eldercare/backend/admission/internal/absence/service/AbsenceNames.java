package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.admission.internal.application.service.M02ActorNames;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Họ tên người đón, nhân viên bàn giao, người thực hiện cho phản hồi lượt vắng (api-004 {@code Absence}). SPI của 008, 012
 * chưa cài thì dùng mã (cùng cách {@link M02ActorNames}).
 */
@Component
public class AbsenceNames {

    private final M02ActorNames accounts;
    private final ObjectProvider<StaffProfileApi> staff;
    private final ObjectProvider<FamilyRelationshipApi> family;

    AbsenceNames(
            M02ActorNames accounts,
            ObjectProvider<StaffProfileApi> staff,
            ObjectProvider<FamilyRelationshipApi> family) {
        this.accounts = accounts;
        this.staff = staff;
        this.family = family;
    }

    public String account(long accountId) {
        return accounts.of(accountId);
    }

    public String staff(long staffId) {
        StaffProfileApi api = staff.getIfAvailable();
        return api == null ? "#" + staffId : api.fullName(staffId).orElse("#" + staffId);
    }

    public String familyMember(long familyMemberId) {
        FamilyRelationshipApi api = family.getIfAvailable();
        return api == null
                ? "#" + familyMemberId
                : api.familyMemberName(familyMemberId).orElse("#" + familyMemberId);
    }
}
