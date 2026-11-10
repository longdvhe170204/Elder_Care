package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.TripIncidentPort;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Bản giả chung do 014 khai port; giữ đầu vào để caller kiểm, không thay bean production. */
public class FakeTripIncidents implements TripIncidentPort {
    private final List<Missing> missing = new CopyOnWriteArrayList<>();
    private final List<Found> found = new CopyOnWriteArrayList<>();
    private boolean failing;

    public record Missing(
            long id, long residentId, long tripId, Long leaderId, String place, Instant at, String description) {}

    public record Found(long incidentId, long residentId, long tripId, Instant at) {}

    public List<Missing> missing() {
        return List.copyOf(missing);
    }

    public List<Found> found() {
        return List.copyOf(found);
    }

    public void failing(boolean value) {
        failing = value;
    }

    public void reset() {
        missing.clear();
        found.clear();
        failing = false;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long recordMissing(
            long residentId, long tripId, Long leaderId, String place, Instant at, String description) {
        if (failing) throw new IllegalStateException("Nguồn sự cố kiểm thử bị lỗi");
        long id = TestUsers.nextId();
        missing.add(new Missing(id, residentId, tripId, leaderId, place, at, description));
        return id;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordFoundReturned(long incidentId, long residentId, long tripId, Instant at) {
        if (failing) throw new IllegalStateException("Nguồn sự cố kiểm thử bị lỗi");
        found.add(new Found(incidentId, residentId, tripId, at));
    }
}
