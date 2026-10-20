package com.cambae.backend.m15.correction.internal;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.correction.api.CreationMethod;
import java.time.Instant;
import java.util.Optional;

/**
 * Chọn cách tạo bản đính chính theo bảng quyết định của data-model mục 4 (FR-026, CHK033, CHK051). Rỗng là không được
 * đính chính (403).
 */
public class CorrectionMethodSelector {

    /** Thông tin về bản gốc và người dùng tại lúc đính chính. */
    public record Facts(long originalRecorder, Optional<Long> floorArea, boolean userCanRecord, Instant at) {}

    private final ScopeApi scope;
    private final AccountApi accounts;

    public CorrectionMethodSelector(ScopeApi scope, AccountApi accounts) {
        this.scope = scope;
        this.accounts = accounts;
    }

    public Optional<CreationMethod> select(CurrentUser user, Facts facts) {
        if (user.accountId() == facts.originalRecorder() && facts.userCanRecord()) {
            return Optional.of(CreationMethod.A);
        }
        boolean sameRole = facts.userCanRecord()
                && accounts.hasRole(facts.originalRecorder(), user.roles().toArray(Role[]::new));
        if (facts.floorArea().isEmpty()) {
            // Không gắn tầng: trưởng tầng, người phụ trách ca không qua cách B (19.3)
            return sameRole ? Optional.of(CreationMethod.C) : Optional.empty();
        }
        long floor = facts.floorArea().get();
        Optional<Long> shiftLead = scope.shiftLead(floor, facts.at());
        Optional<Long> floorManager = scope.assignedFloorManager(floor);
        boolean responsible = shiftLead.filter(id -> id == user.accountId()).isPresent()
                || floorManager.filter(id -> id == user.accountId()).isPresent();
        if (responsible && scope.inScope(user, new Target(null, null, null, floor))) {
            return Optional.of(CreationMethod.B);
        }
        if (shiftLead.isEmpty() && floorManager.isEmpty() && sameRole) {
            return Optional.of(CreationMethod.C);
        }
        return Optional.empty();
    }
}
