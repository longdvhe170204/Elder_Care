package com.cambae.backend.m15.account.scope.internal;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AssignmentKind;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.BedAllocationApi;
import com.cambae.backend.m15.account.api.BedAllocationView;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.DutyApi;
import com.cambae.backend.m15.account.api.FamilyRelationshipApi;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.ResidencyEndApi;
import com.cambae.backend.m15.account.api.ReviewRecipient;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.ScopeFilter;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.permission.internal.FootnoteRules;
import com.cambae.backend.m15.account.permission.internal.PermissionScope;
import com.cambae.backend.m15.account.permission.internal.RolePermissionCalculator;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Fallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Phạm vi dữ liệu (002 T052; research A-07; FR-028 → FR-035a), tính trong từng lần gọi từ phân công của 008 (khoảng
 * CFG-M15-07 quanh ca, {@link ActiveAssignments}) và phân bổ giường đang mở của 003; không đệm giữa các yêu cầu
 * (FR-033). Đối tượng trong phạm vi khi một phân công trùng người cao tuổi, phòng hoặc tầng/khu vực của đối tượng; người
 * phụ trách ca có phạm vi là tầng/khu của ca qua phân công {@code SHIFT_LEAD} (FR-031). Người phụ trách ca, trưởng tầng
 * được giao lấy từ {@code DutyApi} và trả id tài khoản. Chưa có: phạm vi người tham gia chuyến của Trưởng đoàn, Người đi
 * cùng (FR-031; cần SPI của 014), của người cao tuổi (T091, US8). Người thân: người cao tuổi có quan hệ Hiệu lực
 * (012), người đã ở trạng thái cuối còn trong phạm vi tới hết CFG-M01-04 (T071; FR-045, FR-049).
 */
@Service
@Fallback
class ScopeService implements ScopeApi {

    private final BusinessTime time;
    private final ActiveAssignments assignments;
    private final RolePermissionCalculator roles;
    private final JdbcTemplate jdbc;
    // ObjectProvider: 008, 003 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<BedAllocationApi> bedAllocations;
    private final ObjectProvider<DutyApi> duties;
    // 012, 004 cài
    private final ObjectProvider<FamilyRelationshipApi> familyRelationships;
    private final ObjectProvider<ResidencyEndApi> residencyEnds;
    private final ParameterApi parameters;

    ScopeService(
            BusinessTime time,
            ActiveAssignments assignments,
            RolePermissionCalculator roles,
            JdbcTemplate jdbc,
            ObjectProvider<BedAllocationApi> bedAllocations,
            ObjectProvider<DutyApi> duties,
            ObjectProvider<FamilyRelationshipApi> familyRelationships,
            ObjectProvider<ResidencyEndApi> residencyEnds,
            ParameterApi parameters) {
        this.time = time;
        this.assignments = assignments;
        this.roles = roles;
        this.jdbc = jdbc;
        this.bedAllocations = bedAllocations;
        this.duties = duties;
        this.familyRelationships = familyRelationships;
        this.residencyEnds = residencyEnds;
        this.parameters = parameters;
    }

    @Override
    public boolean inScope(CurrentUser user, Target target) {
        return inScope(user, target, time.now());
    }

    @Override
    public boolean inScope(CurrentUser user, Target target, Instant at) {
        if (user.isScheduler()) {
            return true;
        }
        if (user.familyMemberId().isPresent()) {
            return target.residentId() != null
                    && familyResidents(user.familyMemberId().get(), at).contains(target.residentId());
        }
        if (user.staffId().isEmpty()) {
            return false;
        }
        Long floorAreaId = target.floorAreaId();
        Long roomId = null;
        if (target.residentId() != null) {
            Optional<BedAllocationView> bed = assignments.bedOf(target.residentId(), at);
            if (bed.isPresent()) {
                floorAreaId = floorAreaId != null ? floorAreaId : bed.get().floorAreaId();
                roomId = bed.get().roomId();
            }
        }
        for (AssignmentView a : assignments.at(user.staffId().get(), at)) {
            if (matches(a.residentId(), target.residentId())
                    || matches(a.roomId(), roomId)
                    || matches(a.floorAreaId(), floorAreaId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Long assigned, Long ofTarget) {
        return assigned != null && Objects.equals(assigned, ofTarget);
    }

    /**
     * Toàn viện khi một vai trò cho khóa phạm vi toàn viện (FR-034); không thì tầng, phòng, người cao tuổi của phân công
     * hiện hành, {@code residentIds} gồm cả người đang có phân bổ giường mở trong các tầng, phòng đó (FR-029).
     */
    @Override
    public ScopeFilter filter(CurrentUser user, PermissionKey key) {
        if (user.isScheduler() || facilityWide(user, key)) {
            return ScopeFilter.FACILITY;
        }
        if (user.familyMemberId().isPresent()) {
            return family(user.familyMemberId().get(), time.now());
        }
        if (user.staffId().isEmpty()) {
            return empty();
        }
        Instant now = time.now();
        return of(assignments.at(user.staffId().get(), now), now);
    }

    /**
     * Bộ lọc báo cáo khoảng đã qua (FR-035a; Q-192, Q-195): giao trưởng tầng theo các tầng đang được giao lúc xem, gồm
     * cả bản ghi trước khi được giao; phân công khác theo các khoảng góp giao với {@code [from, to)}. {@code residentIds}
     * chỉ gồm người được phân công trực tiếp; module so tầng, phòng của bản ghi lúc phát sinh. Chỉ để đọc (luật kiến
     * trúc trong {@code RequiresPermissionArchTest}).
     */
    @Override
    public ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to) {
        if (user.isScheduler() || facilityWide(user, key)) {
            return ScopeFilter.FACILITY;
        }
        if (user.familyMemberId().isPresent()) {
            return family(user.familyMemberId().get(), time.now());
        }
        if (user.staffId().isEmpty()) {
            return empty();
        }
        long staffId = user.staffId().get();
        List<AssignmentView> current = assignments.at(staffId, time.now()).stream()
                .filter(a -> a.kind() == AssignmentKind.FLOOR_MANAGER)
                .toList();
        List<AssignmentView> past = assignments.during(staffId, from, to).stream()
                .filter(a -> a.kind() != AssignmentKind.FLOOR_MANAGER)
                .toList();
        Set<Long> floors = new HashSet<>();
        Set<Long> rooms = new HashSet<>();
        Set<Long> residents = new HashSet<>();
        collect(current, floors, rooms, residents);
        collect(past, floors, rooms, residents);
        return new ScopeFilter(false, Set.copyOf(floors), Set.copyOf(rooms), Set.copyOf(residents));
    }

    @Override
    public Optional<Long> shiftLead(long floorAreaId, Instant at) {
        DutyApi api = duties.getIfAvailable();
        return api == null ? Optional.empty() : api.shiftLead(floorAreaId, at).flatMap(this::accountOfStaff);
    }

    /** Giao trưởng tầng đang hiệu lực, kể cả giao tạm (Q-84). */
    @Override
    public Optional<Long> assignedFloorManager(long floorAreaId) {
        DutyApi api = duties.getIfAvailable();
        return api == null
                ? Optional.empty()
                : api.assignedFloorManager(floorAreaId, time.now()).flatMap(this::accountOfStaff);
    }

    /**
     * Trưởng tầng được giao của tầng; tầng chưa có thì Người phụ trách ca đang diễn ra (Q-90); bản ghi không gắn tầng
     * thì Quản lý viện (CHK056). Tầng không có cả hai thì Quản lý viện, để bản ghi không bị bỏ lơ (plan 002 ghi chú
     * Phase 6, chủ dự án duyệt 2026-10-21).
     */
    @Override
    public ReviewRecipient reviewDecider(Optional<Long> floorAreaId) {
        if (floorAreaId.isPresent()) {
            long floor = floorAreaId.get();
            Optional<Long> decider = assignedFloorManager(floor)
                    .or(() -> Optional.ofNullable(duties.getIfAvailable())
                            .flatMap(api -> api.floorManagerSubstitute(floor, time.now()))
                            .flatMap(this::accountOfStaff));
            if (decider.isPresent()) {
                return new ReviewRecipient(Set.of(decider.get()));
            }
        }
        return new ReviewRecipient(Set.copyOf(jdbc.queryForList(
                "SELECT a.id FROM account a JOIN role_assignment r ON r.account_id = a.id"
                        + " WHERE r.role = ? AND r.effective_from <= ? AND (r.effective_to IS NULL OR r.effective_to > ?)"
                        + " AND a.status = 'ACTIVE'",
                Long.class,
                Role.NURSING_HOME_MANAGER.code(),
                utc(time.now()),
                utc(time.now()))));
    }

    /** Có phương án toàn viện không vướng chú thích chặn ở lớp vai trò (FR-034). */
    private boolean facilityWide(CurrentUser user, PermissionKey key) {
        return roles.current(user.accountId()).getOrDefault(key, Set.of()).stream()
                .anyMatch(g -> g.scope() == PermissionScope.FACILITY
                        && !FootnoteRules.blockedAtRoleLayer(g.footnotes(), Set.of()));
    }

    private ScopeFilter of(Collection<AssignmentView> active, Instant at) {
        Set<Long> floors = new HashSet<>();
        Set<Long> rooms = new HashSet<>();
        Set<Long> residents = new HashSet<>();
        collect(active, floors, rooms, residents);
        BedAllocationApi beds = bedAllocations.getIfAvailable();
        if (beds != null && (!floors.isEmpty() || !rooms.isEmpty())) {
            residents.addAll(beds.residentsIn(new ScopeFilter(false, floors, rooms, Set.of()), at));
        }
        return new ScopeFilter(false, Set.copyOf(floors), Set.copyOf(rooms), Set.copyOf(residents));
    }

    private static void collect(
            Collection<AssignmentView> from, Set<Long> floors, Set<Long> rooms, Set<Long> residents) {
        for (AssignmentView a : from) {
            if (a.floorAreaId() != null) {
                floors.add(a.floorAreaId());
            }
            if (a.roomId() != null) {
                rooms.add(a.roomId());
            }
            if (a.residentId() != null) {
                residents.add(a.residentId());
            }
        }
    }

    /**
     * Người cao tuổi có quan hệ Hiệu lực với người thân, bỏ người đã ở trạng thái cuối quá CFG-M01-04 tính tới
     * {@code at} (FR-049). Chưa có bản cài 012 thì rỗng; chưa có bản cài 004 thì không ai coi là đã kết thúc.
     */
    private Set<Long> familyResidents(long familyMemberId, Instant at) {
        FamilyRelationshipApi relationships = familyRelationships.getIfAvailable();
        if (relationships == null) {
            return Set.of();
        }
        ResidencyEndApi ends = residencyEnds.getIfAvailable();
        Duration grace = parameters.read(ParameterCode.CFG_M01_04).asDuration();
        return relationships.active(familyMemberId).stream()
                .map(r -> r.residentId())
                .filter(resident -> ends == null
                        || ends.finalStatusAt(resident)
                                .map(end -> !at.isAfter(end.plus(grace)))
                                .orElse(true))
                .collect(Collectors.toUnmodifiableSet());
    }

    private ScopeFilter family(long familyMemberId, Instant at) {
        return new ScopeFilter(false, Set.of(), Set.of(), familyResidents(familyMemberId, at));
    }

    private static ScopeFilter empty() {
        return new ScopeFilter(false, Set.of(), Set.of(), Set.of());
    }

    private Optional<Long> accountOfStaff(long staffId) {
        return jdbc.queryForList("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId).stream()
                .findFirst();
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
