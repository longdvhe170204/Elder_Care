package com.cambae.backend.m15.account.api;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.dto.AccountView;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.Optional;

/**
 * Tra cứu tài khoản và tác động kèm lệnh của module khác (002 contracts/module-interfaces.md mục 1.6). 000 khai báo
 * trước {@code hasRole} (000 T025; plan 004, Q-338); 002 thêm các hàm còn lại (T008).
 */
public interface AccountApi {

    /** Khóa tài khoản khi nhân viên nghỉ việc; gọi trong transaction lệnh Cho nghỉ việc của 008 (FR-009, BR-M09-04). */
    void lockOnResignation(long staffId, Basis basis);

    /** Xét lại tài khoản người thân khi quan hệ chấm dứt; gọi trong transaction lệnh của 012 (FR-049, BR-M15-14). */
    void recheckFamilyMember(long familyMemberId, Basis basis);

    Optional<AccountView> byStaff(long staffId);

    Optional<AccountView> byFamilyMember(long familyMemberId);

    Optional<AccountView> byResident(long residentId);

    boolean hasRole(long accountId, Role... roles);

    /**
     * Họ tên theo hồ sơ liên kết của tài khoản (nhân viên 008, người thân 012; người cao tuổi 001 ở US8); rỗng khi chưa
     * có (002 T032a). Dùng cho {@code Actor.fullName} của 000 và {@code UserInfo.fullName}.
     */
    Optional<String> fullName(long accountId);
}
