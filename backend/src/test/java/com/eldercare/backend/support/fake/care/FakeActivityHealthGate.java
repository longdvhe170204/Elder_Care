package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.ActivityHealthGatePort;
import java.time.Instant;
import java.util.*;

/** Bản giả nguồn 007 cho 014 US2 (khoanh vùng, nghi nhiễm, mục Trung bình trở lên đang mở); mặc định không có gì. */
public class FakeActivityHealthGate implements ActivityHealthGatePort {
    private final FakeM04Switches switches;
    private final Set<Long> isolatedResidents = new HashSet<>();
    private final Set<Long> isolatedRooms = new HashSet<>();
    private final Set<Long> suspected = new HashSet<>();
    private final Set<Long> openItems = new HashSet<>();

    public FakeActivityHealthGate(FakeM04Switches switches) {
        this.switches = switches;
    }

    public synchronized void isolateResident(long residentId) {
        isolatedResidents.add(residentId);
    }

    public synchronized void isolateRoom(long roomId) {
        isolatedRooms.add(roomId);
    }

    public synchronized void suspectInfection(long residentId) {
        suspected.add(residentId);
    }

    public synchronized void openMediumItem(long residentId) {
        openItems.add(residentId);
    }

    @Override
    public synchronized boolean residentIsolated(long residentId, Instant at) {
        switches.requireEnabled("007");
        return isolatedResidents.contains(residentId);
    }

    @Override
    public synchronized boolean locationIsolated(Long roomId, Long areaId, Long floorAreaId, Instant at) {
        switches.requireEnabled("007");
        return (roomId != null && isolatedRooms.contains(roomId)) || (areaId != null && isolatedRooms.contains(areaId));
    }

    @Override
    public synchronized boolean suspectedInfection(long residentId, Instant at) {
        switches.requireEnabled("007");
        return suspected.contains(residentId);
    }

    @Override
    public synchronized boolean hasOpenMediumOrHigher(long residentId) {
        switches.requireEnabled("007");
        return openItems.contains(residentId);
    }
}
