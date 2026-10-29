package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.CapacityViolation;
import com.eldercare.backend.roombedallocation.api.dto.DayCareConfiguration;
import com.eldercare.backend.roombedallocation.api.dto.RegistrationCount;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Hợp đồng 003 DayCareCapacityApi (T003); bản cài thuộc user story tương ứng. */
public interface DayCareCapacityApi {
    @Transactional(propagation = Propagation.MANDATORY)
    DayCareConfiguration lock();

    List<SessionDate> occupiedSessions(LocalDate date, LocalTime arrival, LocalTime departure);

    List<CapacityViolation> checkCapacity(Map<SessionDate, RegistrationCount> counts);
}
