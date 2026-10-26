package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.BedAlertView;
import java.time.Instant;
import java.util.List;

/** Hợp đồng 003 BedHandoverAlertApi (T003); bản cài thuộc user story tương ứng. */
public interface BedHandoverAlertApi {
    List<BedAlertView> byFloor(long floorId, Instant at);
}
