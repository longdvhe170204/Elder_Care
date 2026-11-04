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
import com.eldercare.backend.common.error.NotFoundException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc đồ gửi cho nhân viên (FR-011, FR-012, SC-008; khóa "Đồ gửi" thao tác Xem theo phạm vi). */
@Service
public class BelongingQueries {

    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;

    BelongingQueries(
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users) {
        this.belongings = belongings;
        this.handovers = handovers;
        this.permissions = permissions;
        this.users = users;
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
