package com.cambae.backend.support.fake.m15;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Bản giả {@link DutyApi} (008 cài): nhiệm vụ test đặt sẵn, không xét thời điểm. */
public class FakeDuties implements DutyApi {

    private final Map<Long, Long> shiftLeads = new HashMap<>();
    private final Map<Long, Long> floorManagers = new HashMap<>();
    private final Map<Long, Long> floorManagerSubstitutes = new HashMap<>();
    private final Set<Long> shiftsWithoutFloorManager = new HashSet<>();
    private final Map<Long, Long> departmentLeads = new HashMap<>();
    private final Map<Long, Long> departments = new HashMap<>();
    private final Map<Long, Long> groupLeaders = new HashMap<>();
    private final Map<Long, List<Long>> companions = new HashMap<>();
    private final Map<List<Long>, List<Long>> floorReceivers = new HashMap<>();

    public FakeDuties shiftLead(long floorAreaId, long staffId) {
        shiftLeads.put(floorAreaId, staffId);
        return this;
    }

    public FakeDuties floorManager(long floorAreaId, long staffId) {
        floorManagers.put(floorAreaId, staffId);
        return this;
    }

    public FakeDuties floorManagerSubstitute(long floorAreaId, long staffId) {
        floorManagerSubstitutes.put(floorAreaId, staffId);
        return this;
    }

    public FakeDuties shiftWithoutFloorManager(long shiftId) {
        shiftsWithoutFloorManager.add(shiftId);
        return this;
    }

    public FakeDuties departmentLead(long departmentId, long staffId) {
        departmentLeads.put(departmentId, staffId);
        departments.put(staffId, departmentId);
        return this;
    }

    public FakeDuties department(long staffId, long departmentId) {
        departments.put(staffId, departmentId);
        return this;
    }

    public FakeDuties trip(long tripId, long groupLeaderId, List<Long> companionIds) {
        groupLeaders.put(tripId, groupLeaderId);
        companions.put(tripId, List.copyOf(companionIds));
        return this;
    }

    public FakeDuties floorReceivers(long floorAreaId, long shiftId, List<Long> staffIds) {
        floorReceivers.put(List.of(floorAreaId, shiftId), List.copyOf(staffIds));
        return this;
    }

    @Override
    public Optional<Long> shiftLead(long floorAreaId, Instant at) {
        return Optional.ofNullable(shiftLeads.get(floorAreaId));
    }

    @Override
    public Optional<Long> assignedFloorManager(long floorAreaId, Instant at) {
        return Optional.ofNullable(floorManagers.get(floorAreaId));
    }

    @Override
    public Optional<Long> floorManagerSubstitute(long floorAreaId, Instant at) {
        return Optional.ofNullable(floorManagerSubstitutes.get(floorAreaId));
    }

    @Override
    public boolean shiftHasNoFloorManager(long shiftId) {
        return shiftsWithoutFloorManager.contains(shiftId);
    }

    @Override
    public Optional<Long> departmentLead(long departmentId) {
        return Optional.ofNullable(departmentLeads.get(departmentId));
    }

    @Override
    public boolean isDepartmentLead(long staffId) {
        return departmentLeads.containsValue(staffId);
    }

    @Override
    public Optional<Long> departmentOf(long staffId, Instant at) {
        return Optional.ofNullable(departments.get(staffId));
    }

    @Override
    public Optional<Long> groupLeader(long tripId) {
        return Optional.ofNullable(groupLeaders.get(tripId));
    }

    @Override
    public List<Long> companions(long tripId) {
        return companions.getOrDefault(tripId, List.of());
    }

    /** Bộ phận mà người đang giữ Phụ trách chuyển sang người thay. */
    @Override
    public void replaceDepartmentLead(long currentLeadStaffId, long replacementStaffId, Reason reason) {
        departmentLeads.replaceAll((department, lead) -> lead == currentLeadStaffId ? replacementStaffId : lead);
    }

    @Override
    public List<Long> floorReceivers(long floorAreaId, long shiftId) {
        return floorReceivers.getOrDefault(List.of(floorAreaId, shiftId), List.of());
    }
}
