package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.*;
import com.cambae.backend.m04.api.dto.TaskRequirements;
import java.util.*;

/** Nguồn yêu cầu công việc/kế hoạch và tác động kèm lệnh của C; không thay bằng bean giả trong sản phẩm. */
public class FakeCareRequirements implements TaskTypeApi, CarePlanApi, ShiftTaskApi, TaskHandoverApi {
    private record Scope(long floor, long shift) {}

    private final Map<Scope, List<com.cambae.backend.m04.api.dto.TaskHandoverView>> sources =
            new java.util.concurrent.ConcurrentHashMap<>();
    private Runnable confirmed = () -> {};
    private Runnable resigned = () -> {};
    private java.util.function.BiConsumer<Long, Long> absent = (staff, shift) -> {};

    public void onAbsent(java.util.function.BiConsumer<Long, Long> action) {
        absent = action;
    }

    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void onAbsence(long staffId, long shiftId) {
        absent.accept(staffId, shiftId);
    }

    public void onResign(Runnable action) {
        resigned = action;
    }

    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void onResignation(long staffId, java.time.Instant effectiveAt) {
        resigned.run();
    }

    private Runnable reading = () -> {};

    public void beforeRead(Runnable action) {
        reading = action;
    }

    public void source(long floor, long shift, com.cambae.backend.m04.api.dto.TaskHandoverView row) {
        sources.put(new Scope(floor, shift), List.of(row));
    }

    public List<com.cambae.backend.m04.api.dto.TaskHandoverView> unclosed(long floor, long shift) {
        reading.run();
        return sources.getOrDefault(new Scope(floor, shift), List.of());
    }

    public Map<Long, com.cambae.backend.m04.api.dto.TaskHandoverView> currentStatus(Collection<Long> ids) {
        var result = new HashMap<Long, com.cambae.backend.m04.api.dto.TaskHandoverView>();
        sources.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> ids.contains(r.id()))
                .forEach(r -> result.put(r.id(), r));
        return result;
    }

    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void recordedInHandover(long handover, Collection<Long> ids) {}

    public void onConfirm(Runnable action) {
        confirmed = action;
    }

    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public com.cambae.backend.m04.api.dto.HandoverTransferResult onHandoverConfirmed(
            long handover, long previous, Long next, Set<Long> ids, java.time.Instant at) {
        confirmed.run();
        return new com.cambae.backend.m04.api.dto.HandoverTransferResult(List.of(), List.copyOf(ids));
    }

    private final Map<Long, TaskRequirements> types = new HashMap<>();
    private final Map<Long, Set<Long>> plans = new HashMap<>();
    private final List<Long> ended = new ArrayList<>();
    private boolean failEnd;

    public FakeCareRequirements require(long id, TaskRequirements value) {
        types.put(id, value);
        return this;
    }

    public FakeCareRequirements plan(long resident, Set<Long> taskTypes) {
        plans.put(resident, Set.copyOf(taskTypes));
        return this;
    }

    public FakeCareRequirements failOnEnd(boolean fail) {
        failEnd = fail;
        return this;
    }

    public TaskRequirements requirements(long id) {
        return Optional.ofNullable(types.get(id))
                .orElseThrow(() -> new NoSuchElementException("Thiếu yêu cầu loại công việc trong nguồn test"));
    }

    public Set<Long> currentTaskTypes(long id) {
        return plans.getOrDefault(id, Set.of());
    }

    public void onAssignmentChanged(long resident, long shift) {}

    public void onAssignmentEnded(long assignment, long shift) {
        if (failEnd) throw new IllegalStateException("Nguồn công việc từ chối kết thúc");
        ended.add(assignment);
    }

    public List<Long> endedAssignments() {
        return List.copyOf(ended);
    }
}
