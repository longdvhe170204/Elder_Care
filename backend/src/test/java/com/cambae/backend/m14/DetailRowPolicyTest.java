package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.scope.service.DetailRowPolicy;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.ReviewRecipient;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Giới hạn trường của dòng chi tiết (016 T005; research O-04): FR-012 (Q-196), FR-014, FR-054 (Q-133). */
class DetailRowPolicyTest {

    private static final CurrentUser USER = new ReportScopeTest.User(Set.of(Role.FLOOR_MANAGER));

    private final Set<Long> residentsInScope = Set.of(501L);
    private Visibility medication = Visibility.FULL;

    private final ScopeApi scopes = new ScopeApi() {
        @Override
        public boolean inScope(CurrentUser user, Target target) {
            return residentsInScope.contains(target.residentId());
        }

        @Override
        public ScopeFilter filter(CurrentUser user, PermissionKey key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> shiftLead(long floorAreaId, Instant at) {
            return Optional.empty();
        }

        @Override
        public Optional<Long> assignedFloorManager(long floorAreaId) {
            return Optional.empty();
        }

        @Override
        public ReviewRecipient reviewDecider(Optional<Long> floorAreaId) {
            throw new UnsupportedOperationException();
        }
    };

    private final FieldVisibilityApi fields =
            (user, group, target) -> group == InfoGroup.MEDICATION ? medication : Visibility.FULL;

    private static DetailRow dose(long residentId) {
        return new DetailRow(
                "d1",
                "006",
                "DOSE",
                "MISSED",
                Instant.parse("2026-10-05T01:00:00Z"),
                1L,
                "Tầng 1",
                residentId,
                "Nguyễn Văn A",
                "ĐD B",
                Map.of("medication", "Amlodipin 5 mg", "itemCode", "VT-001"),
                InfoGroup.MEDICATION,
                Map.of("itemCode", "VT-001"),
                false,
                false,
                true);
    }

    private DetailRowPolicy policy() {
        return new DetailRowPolicy(scopes, fields);
    }

    @Test
    void FR_009_residentInScope_rowUnchanged() {
        DetailRow row = policy().apply(USER, List.of(dose(501))).getFirst();

        assertThat(row.reduced()).isFalse();
        assertThat(row.canOpenRecord()).isTrue();
        assertThat(row.result()).containsEntry("medication", "Amlodipin 5 mg");
    }

    @Test
    void FR_012_Q_196_residentOutsideCurrentScope_reducedAndCannotOpen() {
        DetailRow row = policy().apply(USER, List.of(dose(777))).getFirst();

        assertThat(row.reduced()).isTrue();
        assertThat(row.canOpenRecord()).isFalse();
        assertThat(row.resident()).isEqualTo("Nguyễn Văn A");
        assertThat(row.floorArea()).isEqualTo("Tầng 1");
    }

    @Test
    void FR_054_Q_133_withoutMedicationView_onlyItemCodeShown() {
        medication = Visibility.NONE;

        DetailRow row = policy().apply(USER, List.of(dose(501))).getFirst();

        assertThat(row.result()).containsExactly(Map.entry("itemCode", "VT-001"));
    }
}
