package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.BedLocationRow;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.dto.ResidentLocationRow;
import com.cambae.backend.m03.api.dto.RoommateView;
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
