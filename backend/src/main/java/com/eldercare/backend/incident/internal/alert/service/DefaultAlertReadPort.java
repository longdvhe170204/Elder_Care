package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.care.api.spi.AlertReadPort;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật SPI {@link AlertReadPort} của 005 (007 module-interfaces mục 1.1 {@code CanhBaoDocPort}): cảnh báo đang mở
 * của người cao tuổi; cảnh báo theo công việc qua dòng nguồn loại {@code M04.TASK} (research S-08, S-09).
 */
@Service
public class DefaultAlertReadPort implements AlertReadPort {
    static final String TASK_SOURCE = "M04.TASK";

    private final AlertStore alerts;

    public DefaultAlertReadPort(AlertStore alerts) {
        this.alerts = alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertView> open(Collection<Long> residentIds) {
        return alerts.openByResidents(residentIds).stream()
                .map(alert -> new AlertView(alert.id(), alert.residentId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Long>> byTask(Collection<Long> taskIds) {
        var result = new LinkedHashMap<Long, List<Long>>();
        for (long[] row : alerts.bySource(TASK_SOURCE, taskIds)) {
            result.computeIfAbsent(row[0], id -> new ArrayList<>()).add(row[1]);
        }
        result.replaceAll((task, ids) -> List.copyOf(ids));
        return Map.copyOf(result);
    }
}
