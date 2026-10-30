package com.eldercare.backend.roombedallocation.internal.daycare.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.DayCareCapacityApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.internal.daycare.persistence.DayCareStore;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-038–FR-042: không tăng đăng ký khi vượt chỗ hoặc khu nghỉ đang khoanh vùng. */
@Service
public class DayCareCapacityService implements DayCareCapacityApi {
    private final DayCareStore store;
    private final ParameterApi parameters;
    private final LocationStructureApi locations;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final BusinessTime time;

    public DayCareCapacityService(
            DayCareStore store,
            ParameterApi parameters,
            LocationStructureApi locations,
            ObjectProvider<AllocationInfectionStatus> infection,
            BusinessTime time) {
        this.store = store;
        this.parameters = parameters;
        this.locations = locations;
        this.infection = infection;
        this.time = time;
    }

    public int capacity() {
        var value = parameters.read(ParameterCode.CFG_M03_01);
        if (!value.isSet())
            throw new BusinessException(HttpStatus.CONFLICT, "DAY_CARE_CAPACITY_UNSET", "Viện chưa đặt CFG-M03-01");
        return value.asInt();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public DayCareConfiguration lock() {
        store.lock();
        var area = store.activeArea(true)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "DAY_CARE_NOT_CONFIGURED", "Chưa có khu nghỉ Hiệu lực"));
        return new DayCareConfiguration(
                area.id(),
                area.location(),
                capacity(),
                store.sessions(true).stream()
                        .map(x -> x.view())
                        .filter(DayCareSession::active)
                        .toList());
    }

    @Override
    @Transactional
    public List<SessionDate> occupiedSessions(LocalDate date, LocalTime arrival, LocalTime departure) {
        if (date == null || arrival == null || departure == null || !departure.isAfter(arrival))
            throw new InvalidValueException("Giờ về phải sau giờ đến trong cùng ngày");
        store.lock();
        return store.sessions(true).stream()
                .map(x -> x.view())
                .filter(DayCareSession::active)
                .filter(x -> arrival.isBefore(x.endTime()) && departure.isAfter(x.startTime()))
                .map(x -> new SessionDate(date, x.id()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<CapacityViolation> checkCapacity(Map<SessionDate, RegistrationCount> counts) {
        var config = lock();
        var result = new ArrayList<CapacityViolation>();
        for (var item : counts.entrySet()) {
            var key = item.getKey();
            var count = item.getValue();
            var session = config.sessions().stream()
                    .filter(x -> x.id() == key.sessionId())
                    .findFirst()
                    .orElseThrow(() -> new InvalidValueException("Buổi không Hiệu lực"));
            if (count.before() < 0 || count.after() < 0) throw new InvalidValueException("Số đăng ký không âm");
            if (count.after() <= count.before()) continue;
            var source = infection.getIfAvailable();
            if (source == null)
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn khoanh vùng 007");
            var at = key.date()
                    .atTime(session.startTime())
                    .atZone(BusinessTime.ZONE)
                    .toInstant();
            if (at.isBefore(time.now())) at = time.now();
            var cordon = source.cordonedAt(at);
            var location = config.location();
            boolean blocked = "AREA".equals(location.type())
                    ? cordon.areaIds().contains(location.id())
                    : cordon.floorIds().contains(location.id())
                            || locations
                                    .floor(location.id())
                                    .map(x -> cordon.areaIds().contains(x.areaId()))
                                    .orElse(false);
            if (blocked)
                result.add(new CapacityViolation(
                        key, config.capacity(), config.capacity() - count.after(), "DAY_CARE_CORDONED"));
            else if (count.after() > config.capacity())
                result.add(new CapacityViolation(
                        key, config.capacity(), config.capacity() - count.after(), "DAY_CARE_OVER_CAPACITY"));
        }
        return List.copyOf(result);
    }
}
