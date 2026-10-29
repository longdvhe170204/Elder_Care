package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.BedLocationRow;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.dto.ResidentLocationRow;
import com.eldercare.backend.roombedallocation.api.dto.RoommateView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Hợp đồng 003 LocationHistoryApi (T003); bản cài thuộc user story tương ứng. */
public interface LocationHistoryApi {
    List<ResidentLocationRow> byResident(long residentId, Instant from, Instant to);

    List<BedLocationRow> byLocation(LocationRef location, Instant at);

    List<BedLocationRow> byLocationOn(LocationRef location, LocalDate date);

    List<RoommateView> roommates(long residentId, Instant from, Instant to);
}
