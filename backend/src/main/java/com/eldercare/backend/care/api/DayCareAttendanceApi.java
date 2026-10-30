package com.eldercare.backend.care.api;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.care.api.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
public interface DayCareAttendanceApi {
    Optional<AttendanceView> onDate(long residentId, LocalDate date);

    Optional<LocalTime> returnTimeOn(long residentId, LocalDate date);

    List<AttendanceView> inRange(long residentId, LocalDate from, LocalDate to);

    @Transactional(propagation = Propagation.MANDATORY)
    void changeReturnTime(long residentId, LocalDate date, LocalTime newTime, long registrationId);

    @Transactional(propagation = Propagation.MANDATORY)
    void restoreReturnTime(long residentId, LocalDate date, long registrationId);

    @Transactional(propagation = Propagation.MANDATORY)
    void checkOut(
            long residentId, LocalDate date, long pickupRecordId, CheckoutType type, Instant at, CurrentUser user);
}
