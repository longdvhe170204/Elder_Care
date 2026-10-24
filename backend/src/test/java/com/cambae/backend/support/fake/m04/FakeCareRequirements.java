package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.*;
import com.cambae.backend.m04.api.dto.TaskRequirements;
import java.util.*;

/** Nguồn yêu cầu công việc/kế hoạch và tác động kèm lệnh của C; không thay bằng bean giả trong sản phẩm. */
public class FakeCareRequirements implements TaskTypeApi, CarePlanApi, ShiftTaskApi {
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
