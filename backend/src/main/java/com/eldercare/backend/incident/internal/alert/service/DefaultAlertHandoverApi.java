package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.AlertHandoverApi;
import com.eldercare.backend.incident.api.dto.AlertHandoverView;
import com.eldercare.backend.incident.internal.alert.domain.HandoverItem;
import com.eldercare.backend.incident.internal.alert.persistence.HandoverItemStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật {@link AlertHandoverApi} cho 008 (007 module-interfaces mục 1.5, research S-18): danh sách cảnh báo, sự cố
 * đang mở của tầng để người lập bàn giao tự ghi (Q-307, 007 không lập mục bàn giao); chuyển người phụ trách trong
 * transaction xác nhận bàn giao của 008.
 */
@Service
public class DefaultAlertHandoverApi implements AlertHandoverApi {
    private final HandoverItemStore items;
    private final AlertAssigneeTransfer transfer;
    private final BusinessTime time;
    private final Optional<ResidentLocationApi> locations;

    public DefaultAlertHandoverApi(
            HandoverItemStore items,
            AlertAssigneeTransfer transfer,
            BusinessTime time,
            @Autowired(required = false) ResidentLocationApi locations) {
        this.items = items;
        this.transfer = transfer;
        this.time = time;
        this.locations = Optional.ofNullable(locations);
    }

    /** Mọi cảnh báo, sự cố đang mở của tầng; {@code shiftId} không thu hẹp vì mục còn mở đều phải bàn giao (FR-037). */
    @Override
    @Transactional(readOnly = true)
    public List<AlertHandoverView> open(long floorAreaId, long shiftId) {
        var open = items.openByFloor(floorAreaId);
        var rooms = rooms(open);
        return open.stream().map(item -> view(item, rooms)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<SourceRef, AlertHandoverView> currentStatus(Collection<SourceRef> sources) {
        var alertIds = ids(sources, HandoverItem.ALERT);
        var incidentIds = ids(sources, HandoverItem.INCIDENT);
        var found = items.byIds(alertIds, incidentIds);
        var rooms = rooms(found);
        var result = new HashMap<SourceRef, AlertHandoverView>();
        found.forEach(item -> result.put(new SourceRef(item.group(), item.id()), view(item, rooms)));
        return result;
    }

    /** MANDATORY: chạy trong transaction xác nhận bàn giao của 008 (BR-M05-05, BR-M09-08). */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onHandoverConfirmed(long handoverId, long previousShiftId, Long nextShiftId, long floorAreaId) {
        transfer.onConfirmed(previousShiftId, floorAreaId, time.now());
    }

    private static List<Long> ids(Collection<SourceRef> sources, String group) {
        return sources.stream()
                .filter(ref -> group.equals(ref.group()))
                .map(SourceRef::id)
                .distinct()
                .toList();
    }

    private Map<Long, LocationView> rooms(List<HandoverItem> found) {
        var residents = found.stream()
                .map(HandoverItem::residentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (residents.isEmpty()) return Map.of();
        return locations.map(api -> api.current(residents)).orElse(Map.of());
    }

    private static AlertHandoverView view(HandoverItem item, Map<Long, LocationView> rooms) {
        var location = item.residentId() == null ? null : rooms.get(item.residentId());
        return new AlertHandoverView(
                item.id(),
                item.group(),
                item.residentId() == null ? 0 : item.residentId(),
                location == null ? null : location.roomId(),
                item.description(),
                item.status(),
                item.level(),
                item.createdAt());
    }
}
