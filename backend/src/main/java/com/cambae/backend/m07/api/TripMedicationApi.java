package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.TimeInterval;
import com.cambae.backend.m07.api.dto.TripChangeType;

/**
 * API for managing medication carried during outings / hospital transfer (FR-048b, Q-58).
 * Used by Module M04 (014 - Activities and Trips).
 */
public interface TripMedicationApi {
    /** Check whether carried medication for the trip has been prepared (FR-048b). */
    boolean isPrepared(long residentId, long tripId);

    /** Notify that a trip was changed (Canceled, Resident withdrew, Dates changed) to prompt return of medication (Q-58). */
    void onTripChanged(long tripId, long residentId, TripChangeType changeType);

    /** Recalculate interval for carried doses when departure/return time is corrected (FR-048b). */
    void recalculateInterval(long tripId, long residentId, TimeInterval newInterval);
}
