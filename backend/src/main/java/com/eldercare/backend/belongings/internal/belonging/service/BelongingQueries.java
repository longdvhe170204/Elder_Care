package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingDetail;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc đồ gửi cho nhân viên (FR-011, FR-012, SC-008; khóa "Đồ gửi" thao tác Xem theo phạm vi). */
@Service
public class BelongingQueries {

    /** Vai trò có dòng "Đồ gửi" ở ma trận quyền (plan mục "Kế hoạch triển khai R-15" điểm 8, 12). */
    private static final Set<Role> READERS =
            Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.OFFICE_STAFF);

    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final ValuableTypes valuableTypes;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;

    BelongingQueries(
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            ValuableTypes valuableTypes,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users) {
        this.belongings = belongings;
        this.handovers = handovers;
        this.valuableTypes = valuableTypes;
        this.permissions = permissions;
        this.users = users;
    }

    /**
     * Người cao tuổi chủ các đồ trong một lệnh (lệnh chỉ trên đồ của một người, khóa theo người K-03); đồ không có thì 404.
     */
    @Transactional(readOnly = true)
    public long residentOf(Collection<Long> belongingIds) {
        Long resident = null;
        for (Long id : belongingIds) {
            long owner = belongings.residentOf(id).orElseThrow(NotFoundException::new);
            if (resident != null && !Objects.equals(resident, owner)) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "INVALID_VALUE",
                        "Một lệnh bàn giao chỉ gồm đồ của một người cao tuổi");
            }
            resident = owner;
        }
        if (resident == null) {
            throw new NotFoundException();
        }
        return resident;
    }

    /**
     * Đồ có giá trị Đang giữ của một nhân viên (FR-014a) cho người lập bàn giao ca: chính nhân viên đó hoặc vai trò có
     * dòng "Đồ gửi"; lệnh không gắn người cao tuổi nên không đi lớp phạm vi của 002.
     */
    @Transactional(readOnly = true)
    public List<BelongingView> heldValuableBy(long staffId) {
        CurrentUser user = users.getObject();
        boolean self = user.staffId().filter(id -> id == staffId).isPresent();
        if (!self && user.roles().stream().noneMatch(READERS::contains)) {
            throw new PermissionDeniedException("Không xem được đồ gửi nhân viên đang giữ");
        }
        return belongings.heldByStaff(valuableTypes.typeIds(), List.of(staffId));
    }

    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<BelongingView> ofResident(Target resident, BelongingStatus status) {
        return belongings.ofResident(resident.residentId(), status);
    }

    /** Không xem được người cao tuổi chủ đồ thì 404, không lộ đồ có tồn tại (002 FR-043). */
    @Transactional(readOnly = true)
    public BelongingDetail detail(long belongingId) {
        BelongingView belonging = belongings.find(belongingId).orElseThrow(NotFoundException::new);
        boolean allowed = permissions
                .hasPermission(
                        users.getObject(),
                        new PermissionKey(Function.BELONGINGS, Operation.VIEW),
                        new Target("M12.BELONGING", Long.toString(belongingId), belonging.residentId(), null))
                .allowed();
        if (!allowed) {
            throw new NotFoundException();
        }
        long intake = belongings.intakeHandoverOf(belongingId).orElseThrow(NotFoundException::new);
        return new BelongingDetail(belonging, handovers.ofIntake(intake));
    }
}
