package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.spi.AlertReadPort;
import java.util.*;

/** Fixture cảnh báo của 007, không tự sinh cảnh báo hoặc liên kết công việc. */
public class FakeAlertReader implements AlertReadPort {
    private final FakeM04Switches switches;
    private final List<AlertView> alerts = new ArrayList<>();
    private final Map<Long, List<Long>> taskAlerts = new HashMap<>();

    public FakeAlertReader(FakeM04Switches switches) {
        this.switches = switches;
    }

    public void add(AlertView alert) {
        alerts.add(alert);
    }

    public void forTask(long taskId, List<Long> alertIds) {
        taskAlerts.put(taskId, List.copyOf(alertIds));
    }

    @Override
    public List<AlertView> open(Collection<Long> residentIds) {
        switches.requireEnabled("007");
        return alerts.stream().filter(a -> residentIds.contains(a.residentId())).toList();
    }

    @Override
    public Map<Long, List<Long>> byTask(Collection<Long> taskIds) {
        switches.requireEnabled("007");
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (long id : taskIds) if (taskAlerts.containsKey(id)) result.put(id, taskAlerts.get(id));
        return Map.copyOf(result);
    }

    public void reset() {
        alerts.clear();
        taskAlerts.clear();
    }
}
